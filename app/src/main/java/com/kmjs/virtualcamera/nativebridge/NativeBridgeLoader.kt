package com.kmjs.virtualcamera.nativebridge

import android.os.Build
import com.kmjs.virtualcamera.core.DiagnosticsLogger

object NativeBridgeLoader {

    private var isNativeLoaded = false

    fun loadNativeLibraries(): Boolean {
        if (isNativeLoaded) return true

        val abi = Build.SUPPORTED_ABIS.firstOrNull() ?: "unknown"
        DiagnosticsLogger.module("NativeBridgeLoader: Loading acceleration JNI bridge for ABI: $abi")

        try {
            // Attempt to load native accelerated library if packaged
            System.loadLibrary("kmjs_virtualcamera")
            isNativeLoaded = true
            DiagnosticsLogger.module("Native library 'libkmjs_virtualcamera.so' loaded successfully.")
            return true
        } catch (_: UnsatisfiedLinkError) {
            // Software fallback
            DiagnosticsLogger.module("Native library 'libkmjs_virtualcamera.so' not found; using pure-Kotlin software pipeline.")
            isNativeLoaded = false
            return false
        }
    }

    fun isLoaded(): Boolean = isNativeLoaded
}
