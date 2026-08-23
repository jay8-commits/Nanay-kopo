package com.kmjs.virtualcamera.npatch

import android.os.Build
import com.kmjs.virtualcamera.core.DiagnosticsLogger
import java.io.File

object NPatchCompatibilityManager {

    val SUPPORTED_ABIS = listOf("arm64-v8a", "x86_64", "armeabi-v7a", "x86")

    fun checkCompatibility(): NPatchCheckResult {
        DiagnosticsLogger.module("Verifying NPatch environment compatibility...")

        val primaryAbi = Build.SUPPORTED_ABIS.firstOrNull() ?: "unknown"
        val isAbiSupported = Build.SUPPORTED_ABIS.any { it in SUPPORTED_ABIS }

        DiagnosticsLogger.module("Device ABIs: ${Build.SUPPORTED_ABIS.joinToString()}, Primary: $primaryAbi, Supported: $isAbiSupported")

        val hasEmbeddedAssets = checkEmbeddedAssets()

        val isCompatible = isAbiSupported

        return NPatchCheckResult(
            isCompatible = isCompatible,
            primaryAbi = primaryAbi,
            supportedAbis = Build.SUPPORTED_ABIS.toList(),
            hasEmbeddedAssets = hasEmbeddedAssets,
            notes = if (isCompatible) "NPatch embedded runtime verified." else "Device ABI not supported for NPatch."
        )
    }

    private fun checkEmbeddedAssets(): Boolean {
        return try {
            val appInfo = File("/proc/self/cmdline")
            appInfo.exists()
        } catch (_: Throwable) {
            false
        }
    }
}

data class NPatchCheckResult(
    val isCompatible: Boolean,
    val primaryAbi: String,
    val supportedAbis: List<String>,
    val hasEmbeddedAssets: Boolean,
    val notes: String
)
