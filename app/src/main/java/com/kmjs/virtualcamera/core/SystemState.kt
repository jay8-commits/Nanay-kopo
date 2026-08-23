package com.kmjs.virtualcamera.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class KMJSSystemState(
    val isConnected: Boolean = false,
    val detectedPackage: String = "com.photo.android.camera",
    val detectedProcess: String = "com.photo.android.camera",
    val targetSupported: Boolean = true,
    val cameraApi: CameraApiType = CameraApiType.CAMERA2,
    val hookStatus: HookStatus = HookStatus.INSTALLED,
    val streamStatus: StreamStatus = StreamStatus.DISCONNECTED,
    val decoderStatus: DecoderStatus = DecoderStatus.STOPPED,
    val framesReceived: Long = 0L,
    val framesDelivered: Long = 0L,
    val currentFps: Float = 0.0f,
    val latencyMs: Long = 0L,
    val droppedFrames: Long = 0L,
    val lastFailureCode: FailSafeCode? = null,
    val lastErrorMessage: String? = null
)

enum class FailSafeCode(val code: String, val message: String) {
    TARGET_UNSUPPORTED("TARGET_UNSUPPORTED", "Target application package is not supported or disabled"),
    CAMERA_API_UNSUPPORTED("CAMERA_API_UNSUPPORTED", "Camera API used by target is not supported"),
    HOOK_INSTALL_FAILED("HOOK_INSTALL_FAILED", "Failed to install runtime hooks into target camera pipeline"),
    RTSP_CONNECTION_FAILED("RTSP_CONNECTION_FAILED", "Unable to establish RTSP connection or negotiate RTP session"),
    DECODER_FAILED("DECODER_FAILED", "MediaCodec or software video decoder failed to initialize or decode stream"),
    FRAME_DELIVERY_FAILED("FRAME_DELIVERY_FAILED", "Virtual frame provider failed to deliver frame to target surface")
}

object StateRepository {
    private val _systemState = MutableStateFlow(KMJSSystemState())
    val systemState: StateFlow<KMJSSystemState> = _systemState.asStateFlow()

    private val _config = MutableStateFlow(VirtualCameraConfig())
    val config: StateFlow<VirtualCameraConfig> = _config.asStateFlow()

    fun updateConfig(newConfig: VirtualCameraConfig) {
        _config.value = newConfig
    }

    fun updateState(transform: (KMJSSystemState) -> KMJSSystemState) {
        _systemState.update(transform)
    }

    fun reportFailure(code: FailSafeCode, details: String, process: String = "com.kmjs.virtualcamera", throwable: Throwable? = null) {
        DiagnosticsLogger.error("[${code.code}] $details: ${throwable?.message ?: ""}", process = process, throwable = throwable)
        _systemState.update { current ->
            current.copy(
                lastFailureCode = code,
                lastErrorMessage = "${code.code}: $details"
            )
        }
    }
}
