package com.kmjs.virtualcamera.runtime

import com.kmjs.virtualcamera.core.CameraApiType
import com.kmjs.virtualcamera.core.DiagnosticsLogger

data class CameraApiDetectionResult(
    val primaryApi: CameraApiType,
    val hasCamera2: Boolean,
    val hasCameraX: Boolean,
    val hasLegacy: Boolean,
    val details: String
)

object CameraApiDetector {

    fun detectCameraApi(classLoader: ClassLoader = javaClass.classLoader!!): CameraApiDetectionResult {
        DiagnosticsLogger.camera("Scanning target classloader for active Camera APIs...")

        var hasCamera2 = false
        var hasCameraX = false
        var hasLegacy = false

        try {
            Class.forName("android.hardware.camera2.CameraDevice", false, classLoader)
            hasCamera2 = true
        } catch (_: Throwable) {}

        try {
            Class.forName("androidx.camera.core.Camera", false, classLoader)
            hasCameraX = true
        } catch (_: Throwable) {
            try {
                Class.forName("androidx.camera.core.ImageAnalysis", false, classLoader)
                hasCameraX = true
            } catch (_: Throwable) {}
        }

        try {
            Class.forName("android.hardware.Camera", false, classLoader)
            hasLegacy = true
        } catch (_: Throwable) {}

        // Determine primary API by architecture priority: Camera2 -> CameraX -> Legacy
        val primaryApi = when {
            hasCamera2 -> CameraApiType.CAMERA2
            hasCameraX -> CameraApiType.CAMERAX
            hasLegacy -> CameraApiType.LEGACY
            else -> CameraApiType.UNKNOWN
        }

        val details = buildString {
            append("Detected APIs: [")
            if (hasCamera2) append("Camera2 ")
            if (hasCameraX) append("CameraX ")
            if (hasLegacy) append("LegacyCamera ")
            append("] => Primary selected: ${primaryApi.displayName}")
        }

        DiagnosticsLogger.camera("API Detection result: $details")

        return CameraApiDetectionResult(
            primaryApi = primaryApi,
            hasCamera2 = hasCamera2,
            hasCameraX = hasCameraX,
            hasLegacy = hasLegacy,
            details = details
        )
    }
}
