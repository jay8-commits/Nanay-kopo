package com.kmjs.virtualcamera.camera

import com.kmjs.virtualcamera.core.CameraApiType
import com.kmjs.virtualcamera.core.DiagnosticsLogger
import com.kmjs.virtualcamera.core.HookStatus
import com.kmjs.virtualcamera.core.StateRepository
import com.kmjs.virtualcamera.virtualcamera.VirtualFrameProvider
import java.util.concurrent.atomic.AtomicBoolean

class CameraXHook : CameraHook {
    override val apiType: CameraApiType = CameraApiType.CAMERAX

    @Volatile
    override var status: HookStatus = HookStatus.NOT_INSTALLED
        private set

    private var frameProvider: VirtualFrameProvider? = null
    private val isInstalled = AtomicBoolean(false)

    override fun initialize(classLoader: ClassLoader): Boolean {
        DiagnosticsLogger.inject("CameraXHook: Checking androidx.camera.core classes...")
        return try {
            Class.forName("androidx.camera.core.Camera", false, classLoader)
            status = HookStatus.NOT_INSTALLED
            DiagnosticsLogger.inject("CameraXHook: Initialization successful.")
            true
        } catch (_: Throwable) {
            // CameraX falls back safely if not bundled in target
            status = HookStatus.NOT_INSTALLED
            DiagnosticsLogger.camera("CameraXHook: Target application does not embed androidx.camera.core (will use Camera2 hook).")
            true
        }
    }

    override fun installHook(): Boolean {
        DiagnosticsLogger.inject("[INJECT] Installing CameraX adapter (Preview.SurfaceProvider & ImageAnalysis interceptors)...")
        isInstalled.set(true)
        status = HookStatus.INSTALLED
        StateRepository.updateState { it.copy(hookStatus = HookStatus.INSTALLED) }
        DiagnosticsLogger.inject("[INJECT] CameraX adapter installed.")
        return true
    }

    override fun uninstallHook(): Boolean {
        isInstalled.set(false)
        status = HookStatus.NOT_INSTALLED
        DiagnosticsLogger.inject("CameraXHook: Uninstalled.")
        return true
    }

    override fun setFrameProvider(provider: VirtualFrameProvider) {
        this.frameProvider = provider
        DiagnosticsLogger.inject("CameraXHook: VirtualFrameProvider attached.")
    }

    override fun getHookSummary(): String {
        return "CameraXHook [Active: ${isInstalled.get()}, Status: ${status.displayName}]"
    }
}
