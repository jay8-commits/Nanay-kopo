package com.kmjs.virtualcamera.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

object RuntimeLifecycleManager {
    private val scope = CoroutineScope(Dispatchers.Default)
    private var healthCheckJob: Job? = null

    fun startMonitoring() {
        if (healthCheckJob?.isActive == true) return
        DiagnosticsLogger.module("RuntimeLifecycleManager monitoring started")
        healthCheckJob = scope.launch {
            while (isActive) {
                delay(1000L)
                val state = StateRepository.systemState.value
                // If stream is connected and decoder is running, update stats
                if (state.streamStatus == StreamStatus.CONNECTED && state.decoderStatus == DecoderStatus.RUNNING) {
                    // Update state safely
                }
            }
        }
    }

    fun stopMonitoring() {
        healthCheckJob?.cancel()
        healthCheckJob = null
        DiagnosticsLogger.module("RuntimeLifecycleManager monitoring stopped")
    }

    fun handleSafeFailure(code: FailSafeCode, details: String, process: String = "KMJS_MODULE", throwable: Throwable? = null) {
        StateRepository.reportFailure(code, details, process)
        if (throwable != null) {
            DiagnosticsLogger.error("$details: ${throwable.message}", process = process, throwable = throwable)
        }
        when (code) {
            FailSafeCode.TARGET_UNSUPPORTED -> {
                StateRepository.updateState { it.copy(targetSupported = false, hookStatus = HookStatus.NOT_INSTALLED) }
            }
            FailSafeCode.CAMERA_API_UNSUPPORTED -> {
                StateRepository.updateState { it.copy(hookStatus = HookStatus.UNSUPPORTED) }
            }
            FailSafeCode.HOOK_INSTALL_FAILED -> {
                StateRepository.updateState { it.copy(hookStatus = HookStatus.FAILED) }
            }
            FailSafeCode.RTSP_CONNECTION_FAILED -> {
                StateRepository.updateState { it.copy(streamStatus = StreamStatus.ERROR) }
            }
            FailSafeCode.DECODER_FAILED -> {
                StateRepository.updateState { it.copy(decoderStatus = DecoderStatus.FAILED) }
            }
            FailSafeCode.FRAME_DELIVERY_FAILED -> {
                DiagnosticsLogger.frame("Frame delivery degradation detected; maintaining fail-safe pipeline")
            }
        }
    }
}
