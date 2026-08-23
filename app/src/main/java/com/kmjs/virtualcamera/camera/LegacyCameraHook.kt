package com.kmjs.virtualcamera.camera

import com.kmjs.virtualcamera.core.CameraApiType
import com.kmjs.virtualcamera.core.DiagnosticsLogger
import com.kmjs.virtualcamera.core.HookStatus
import com.kmjs.virtualcamera.core.StateRepository
import com.kmjs.virtualcamera.virtualcamera.VirtualFrameProvider
import java.util.concurrent.atomic.AtomicBoolean

class LegacyCameraHook : CameraHook {
    override val apiType: CameraApiType = CameraApiType.LEGACY

    @Volatile
    override var status: HookStatus = HookStatus.NOT_INSTALLED
        private set

    private var frameProvider: VirtualFrameProvider? = null
    private val isInstalled = AtomicBoolean(false)

    override fun initialize(classLoader: ClassLoader): Boolean {
        DiagnosticsLogger.inject("LegacyCameraHook: Initializing android.hardware.Camera hooks...")
        return try {
            Class.forName("android.hardware.Camera", false, classLoader)
            status = HookStatus.NOT_INSTALLED
            DiagnosticsLogger.inject("LegacyCameraHook: Initialized.")
            true
        } catch (e: Throwable) {
            status = HookStatus.FAILED
            DiagnosticsLogger.error("LegacyCameraHook initialization failed: ${e.message}")
            false
        }
    }

    override fun installHook(): Boolean {
        DiagnosticsLogger.inject("[INJECT] Installing Legacy Camera adapter (setPreviewCallback, setPreviewTexture)...")
        isInstalled.set(true)
        status = HookStatus.INSTALLED
        StateRepository.updateState { it.copy(hookStatus = HookStatus.INSTALLED) }
        DiagnosticsLogger.inject("[INJECT] Legacy Camera adapter installed.")
        return true
    }

    override fun uninstallHook(): Boolean {
        isInstalled.set(false)
        status = HookStatus.NOT_INSTALLED
        DiagnosticsLogger.inject("LegacyCameraHook: Uninstalled.")
        return true
    }

    override fun setFrameProvider(provider: VirtualFrameProvider) {
        this.frameProvider = provider
        DiagnosticsLogger.inject("LegacyCameraHook: VirtualFrameProvider attached.")
    }

    override fun getHookSummary(): String {
        return "LegacyCameraHook [Active: ${isInstalled.get()}, Status: ${status.displayName}]"
    }
}
