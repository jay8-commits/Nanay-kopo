package com.kmjs.virtualcamera.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.atomic.AtomicLong

enum class LogCategory(val tag: String) {
    MODULE("MODULE"),
    PROCESS("PROCESS"),
    TARGET("TARGET"),
    CAMERA("CAMERA"),
    INJECT("INJECT"),
    RTSP("RTSP"),
    DECODER("DECODER"),
    FRAME("FRAME"),
    ERROR("ERROR")
}

data class LogEntry(
    val id: Long,
    val timestamp: Long,
    val category: LogCategory,
    val process: String,
    val message: String,
    val throwableStackTrace: String? = null
) {
    val formattedTime: String
        get() = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date(timestamp))

    fun toFormattedString(): String {
        val base = "[$formattedTime] [${category.tag}] [$process] $message"
        return if (throwableStackTrace != null) {
            "$base\n$throwableStackTrace"
        } else {
            base
        }
    }
}

object DiagnosticsLogger {
    private const val MAX_LOGS = 600
    private val idGenerator = AtomicLong(1)
    private val logDeque = ConcurrentLinkedDeque<LogEntry>()
    private val _logsFlow = MutableStateFlow<List<LogEntry>>(emptyList())
    val logsFlow: StateFlow<List<LogEntry>> = _logsFlow.asStateFlow()

    @Volatile
    var currentProcessName: String = "com.kmjs.virtualcamera"

    fun log(category: LogCategory, message: String, process: String = currentProcessName, throwable: Throwable? = null) {
        val stackTrace = throwable?.let {
            val sw = StringWriter()
            it.printStackTrace(PrintWriter(sw))
            sw.toString()
        }

        val entry = LogEntry(
            id = idGenerator.getAndIncrement(),
            timestamp = System.currentTimeMillis(),
            category = category,
            process = process,
            message = message,
            throwableStackTrace = stackTrace
        )

        logDeque.addFirst(entry)
        while (logDeque.size > MAX_LOGS) {
            logDeque.pollLast()
        }
        _logsFlow.value = logDeque.toList()
    }

    fun module(message: String, process: String = currentProcessName) = log(LogCategory.MODULE, message, process)
    fun process(message: String, process: String = currentProcessName) = log(LogCategory.PROCESS, message, process)
    fun target(message: String, process: String = currentProcessName) = log(LogCategory.TARGET, message, process)
    fun camera(message: String, process: String = currentProcessName) = log(LogCategory.CAMERA, message, process)
    fun inject(message: String, process: String = currentProcessName) = log(LogCategory.INJECT, message, process)
    fun rtsp(message: String, process: String = currentProcessName) = log(LogCategory.RTSP, message, process)
    fun decoder(message: String, process: String = currentProcessName) = log(LogCategory.DECODER, message, process)
    fun frame(message: String, process: String = currentProcessName) = log(LogCategory.FRAME, message, process)
    fun error(message: String, process: String = currentProcessName, throwable: Throwable? = null) =
        log(LogCategory.ERROR, message, process, throwable)

    fun clear() {
        logDeque.clear()
        _logsFlow.value = emptyList()
        module("Diagnostics log buffer cleared")
    }

    fun exportAllLogs(): String {
        val sb = StringBuilder()
        sb.appendLine("=== KMJS Virtual Camera Diagnostics Log Export ===")
        sb.appendLine("Exported at: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())}")
        sb.appendLine("Total entries: ${logDeque.size}")
        sb.appendLine("==================================================")
        logDeque.toList().reversed().forEach { entry ->
            sb.appendLine(entry.toFormattedString())
        }
        return sb.toString()
    }
}
