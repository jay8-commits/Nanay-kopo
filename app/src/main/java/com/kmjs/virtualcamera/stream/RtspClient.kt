package com.kmjs.virtualcamera.stream

import com.kmjs.virtualcamera.core.DiagnosticsLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URI
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

enum class RtspState {
    DISCONNECTED,
    CONNECTING,
    OPTIONS_SENT,
    DESCRIBE_SENT,
    SETUP_SENT,
    PLAYING,
    ERROR
}

class RtspClient(
    private val onPacketReceived: (ByteArray, Int, Long) -> Unit,
    private val onStateChanged: (RtspState, String?) -> Unit
) {
    private var socket: Socket? = null
    private var outputStream: OutputStream? = null
    private var inputStream: BufferedReader? = null
    private val cSeq = AtomicInteger(1)
    private val isRunning = AtomicBoolean(false)
    private var clientJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    @Volatile
    var currentState: RtspState = RtspState.DISCONNECTED
        private set

    fun connect(url: String, username: String? = null, password: String? = null) {
        if (isRunning.get()) {
            disconnect()
        }
        isRunning.set(true)
        updateState(RtspState.CONNECTING, "Connecting to $url")
        DiagnosticsLogger.rtsp("[KMJS_RTSP_CONNECT] Connecting to RTSP source: $url")

        clientJob = scope.launch {
            try {
                val uri = URI(url)
                val host = uri.host ?: "127.0.0.1"
                val port = if (uri.port > 0) uri.port else 554
                val path = uri.path ?: ""

                DiagnosticsLogger.rtsp("[KMJS_RTSP_CONNECT] Opening TCP socket to $host:$port...")
                val sock = Socket()
                sock.connect(InetSocketAddress(host, port), 5000)
                sock.soTimeout = 6000
                socket = sock
                outputStream = sock.getOutputStream()
                inputStream = BufferedReader(InputStreamReader(sock.getInputStream()))

                DiagnosticsLogger.rtsp("Socket connected. Initiating RTSP handshake (OPTIONS/DESCRIBE/SETUP/PLAY)...")
                sendOptions(url)
                sendDescribe(url)
                sendSetup(url)
                sendPlay(url)

                updateState(RtspState.PLAYING, "RTSP stream playing")
                DiagnosticsLogger.rtsp("[KMJS_RTSP_CONNECTED] RTSP stream connected and active: $url (Transport: RTP/AVP/TCP)")

                // Read stream loop
                val buffer = ByteArray(4096)
                val rawIn = sock.getInputStream()
                while (isRunning.get() && isActive) {
                    val read = rawIn.read(buffer)
                    if (read == -1) break
                    if (read > 0) {
                        onPacketReceived(buffer, read, System.currentTimeMillis() * 1_000_000L)
                    }
                }
            } catch (e: Exception) {
                DiagnosticsLogger.error("[KMJS_RTSP_ERROR] RTSP network connection failed for $url: ${e.message}")
                updateState(RtspState.ERROR, e.message)
            }
        }
    }

    private fun sendOptions(url: String) {
        val req = "OPTIONS $url RTSP/1.0\r\nCSeq: ${cSeq.getAndIncrement()}\r\nUser-Agent: KMJS-VirtualCamera/1.0\r\n\r\n"
        outputStream?.write(req.toByteArray())
        outputStream?.flush()
    }

    private fun sendDescribe(url: String) {
        val req = "DESCRIBE $url RTSP/1.0\r\nCSeq: ${cSeq.getAndIncrement()}\r\nAccept: application/sdp\r\nUser-Agent: KMJS-VirtualCamera/1.0\r\n\r\n"
        outputStream?.write(req.toByteArray())
        outputStream?.flush()
    }

    private fun sendSetup(url: String) {
        val req = "SETUP $url/trackID=0 RTSP/1.0\r\nCSeq: ${cSeq.getAndIncrement()}\r\nTransport: RTP/AVP/TCP;unicast;interleaved=0-1\r\nUser-Agent: KMJS-VirtualCamera/1.0\r\n\r\n"
        outputStream?.write(req.toByteArray())
        outputStream?.flush()
    }

    private fun sendPlay(url: String) {
        val req = "PLAY $url RTSP/1.0\r\nCSeq: ${cSeq.getAndIncrement()}\r\nRange: npt=0.000-\r\nUser-Agent: KMJS-VirtualCamera/1.0\r\n\r\n"
        outputStream?.write(req.toByteArray())
        outputStream?.flush()
    }

    fun disconnect() {
        isRunning.set(false)
        clientJob?.cancel()
        clientJob = null
        try {
            socket?.close()
        } catch (_: Throwable) {}
        socket = null
        outputStream = null
        inputStream = null
        updateState(RtspState.DISCONNECTED, "RTSP disconnected")
        DiagnosticsLogger.rtsp("RTSP client disconnected")
    }

    private fun updateState(state: RtspState, msg: String?) {
        currentState = state
        onStateChanged(state, msg)
    }
}
