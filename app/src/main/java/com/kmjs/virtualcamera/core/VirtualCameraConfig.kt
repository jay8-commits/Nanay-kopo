package com.kmjs.virtualcamera.core

enum class PixelFormat(val displayName: String, val bytesPerPixel: Float) {
    RGBA_8888("RGBA 8888", 4.0f),
    NV21("NV21 (YUV 420)", 1.5f),
    YUV_420_888("YUV 420 888", 1.5f),
    RGB_565("RGB 565", 2.0f)
}

enum class CameraApiType(val displayName: String) {
    CAMERA2("Camera2"),
    CAMERAX("CameraX"),
    LEGACY("Legacy Camera"),
    UNKNOWN("Unknown / Not Detected")
}

enum class HookStatus(val displayName: String) {
    NOT_INSTALLED("Not Installed"),
    INSTALLING("Installing"),
    INSTALLED("Installed (Active)"),
    FAILED("Failed"),
    UNSUPPORTED("Unsupported API")
}

enum class StreamStatus(val displayName: String) {
    DISCONNECTED("Disconnected"),
    CONNECTING("Connecting"),
    CONNECTED("Connected"),
    RECONNECTING("Reconnecting"),
    ERROR("Error")
}

enum class DecoderStatus(val displayName: String) {
    STOPPED("Stopped"),
    STARTING("Starting"),
    RUNNING("Running"),
    DEGRADED("Degraded (Dropping Frames)"),
    FAILED("Failed")
}

data class VirtualCameraConfig(
    val rtspUrl: String = "rtsp://9627b0bf2a7b.entrypoint.cloud.wowza.com:1935/app-p5260J38/66abe4b9_stream1",
    val username: String = "",
    val password: String = "",
    val width: Int = 1280,
    val height: Int = 720,
    val fps: Int = 30,
    val rotation: Int = 0,
    val pixelFormat: PixelFormat = PixelFormat.RGBA_8888,
    val testPatternEnabled: Boolean = false,
    val autoReconnect: Boolean = true,
    val reconnectIntervalMs: Long = 3000L,
    val bufferCapacity: Int = 8,
    val dropSlowFrames: Boolean = true
)
