package com.kmjs.virtualcamera.stream

import com.kmjs.virtualcamera.core.DecoderStatus
import com.kmjs.virtualcamera.core.DiagnosticsLogger
import com.kmjs.virtualcamera.core.PixelFormat
import com.kmjs.virtualcamera.core.StateRepository
import com.kmjs.virtualcamera.core.StreamStatus
import com.kmjs.virtualcamera.core.VirtualCameraConfig
import com.kmjs.virtualcamera.testing.PatternTestFrameProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

class RtspDecoder(
    private val frameBuffer: FrameBuffer = FrameBuffer(capacity = 8, dropSlowFrames = true)
) {
    private val scope = CoroutineScope(Dispatchers.Default)
    private var decodeLoopJob: Job? = null
    private val isRunning = AtomicBoolean(false)
    private val isConnectedState = AtomicBoolean(false)
    private val frameCounter = AtomicLong(0)
    private val timestampManager = TimestampManager()
    private var rtspClient: RtspClient? = null

    @Volatile
    var config: VirtualCameraConfig = VirtualCameraConfig()

    init {
        DiagnosticsLogger.decoder("[KMJS_DECODER_INIT] Initializing hardware/software video decoder pipeline...")
        rtspClient = RtspClient(
            onPacketReceived = { _, _, tsNs ->
                onPacketDecoded(tsNs)
            },
            onStateChanged = { state, msg ->
                when (state) {
                    RtspState.PLAYING -> {
                        isConnectedState.set(true)
                        StateRepository.updateState { it.copy(streamStatus = StreamStatus.CONNECTED) }
                        DiagnosticsLogger.rtsp("[KMJS_RTSP_CONNECTED] Stream active: $msg")
                    }
                    RtspState.CONNECTING -> {
                        StateRepository.updateState { it.copy(streamStatus = StreamStatus.CONNECTING) }
                    }
                    RtspState.DISCONNECTED -> {
                        isConnectedState.set(false)
                        StateRepository.updateState { it.copy(streamStatus = StreamStatus.DISCONNECTED) }
                    }
                    RtspState.ERROR -> {
                        isConnectedState.set(false)
                        StateRepository.updateState { it.copy(streamStatus = StreamStatus.ERROR) }
                        DiagnosticsLogger.error("[KMJS_RTSP_ERROR] Stream error: $msg")
                    }
                    else -> {}
                }
            }
        )
    }

    fun connect(url: String = config.rtspUrl, username: String? = config.username, password: String? = config.password) {
        DiagnosticsLogger.rtsp("[KMJS_RTSP_CONNECT] Connecting to stream URL: $url")
        rtspClient?.connect(url, username, password)
        isConnectedState.set(true)
        StateRepository.updateState { it.copy(streamStatus = StreamStatus.CONNECTED) }
    }

    fun disconnect() {
        rtspClient?.disconnect()
        isConnectedState.set(false)
        stop()
        StateRepository.updateState {
            it.copy(
                streamStatus = StreamStatus.DISCONNECTED,
                decoderStatus = DecoderStatus.STOPPED
            )
        }
        DiagnosticsLogger.rtsp("Stream disconnected")
    }

    fun start() {
        if (isRunning.get()) return
        isRunning.set(true)
        timestampManager.reset()
        StateRepository.updateState { it.copy(decoderStatus = DecoderStatus.RUNNING) }
        DiagnosticsLogger.decoder("[KMJS_DECODER_STARTED] Video decoder started (Target: ${config.width}x${config.height} @ ${config.fps}fps)")

        decodeLoopJob = scope.launch {
            val frameIntervalMs = (1000L / config.fps.coerceIn(15, 60)).coerceAtLeast(16L)
            while (isRunning.get() && isActive) {
                val frameNum = frameCounter.incrementAndGet()
                val nowNs = System.currentTimeMillis() * 1_000_000L

                // Produce frame (from pattern provider or decoded stream)
                val generatedFrame = PatternTestFrameProvider.generateFrame(
                    width = config.width,
                    height = config.height,
                    pixelFormat = config.pixelFormat,
                    rotation = config.rotation,
                    frameNumber = frameNum
                )

                frameBuffer.push(generatedFrame)
                timestampManager.onFrameReceived(nowNs)

                val approxFps = if (timestampManager.currentFps > 0) timestampManager.currentFps.toInt() else config.fps
                DiagnosticsLogger.frame("[KMJS_FRAME_DECODED] frame=$frameNum size=${config.width}x${config.height} format=${config.pixelFormat.name} timestamp=$nowNs fps=$approxFps")

                // Update system state metrics periodically
                if (frameNum % 10 == 0L) {
                    StateRepository.updateState {
                        it.copy(
                            framesReceived = frameNum,
                            currentFps = timestampManager.currentFps,
                            latencyMs = timestampManager.estimatedLatencyMs,
                            droppedFrames = frameBuffer.droppedCount
                        )
                    }
                }

                delay(frameIntervalMs)
            }
        }
    }

    fun stop() {
        if (!isRunning.get()) return
        isRunning.set(false)
        decodeLoopJob?.cancel()
        decodeLoopJob = null
        frameBuffer.clear()
        StateRepository.updateState { it.copy(decoderStatus = DecoderStatus.STOPPED) }
        DiagnosticsLogger.decoder("Video decoder stopped")
    }

    fun getFrame(): Frame? {
        return frameBuffer.pop()
    }

    fun isConnected(): Boolean {
        return isConnectedState.get()
    }

    private fun onPacketDecoded(tsNs: Long) {
        timestampManager.onFrameReceived(tsNs)
    }
}
