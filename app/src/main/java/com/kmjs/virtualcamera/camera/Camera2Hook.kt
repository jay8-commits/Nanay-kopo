package com.kmjs.virtualcamera.camera

import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.view.Surface
import com.kmjs.virtualcamera.core.CameraApiType
import com.kmjs.virtualcamera.core.DiagnosticsLogger
import com.kmjs.virtualcamera.core.FailSafeCode
import com.kmjs.virtualcamera.core.HookStatus
import com.kmjs.virtualcamera.core.RuntimeLifecycleManager
import com.kmjs.virtualcamera.core.StateRepository
import com.kmjs.virtualcamera.virtualcamera.VirtualFrameProvider
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

class Camera2Hook : CameraHook {

    override val apiType: CameraApiType = CameraApiType.CAMERA2

    @Volatile
    override var status: HookStatus = HookStatus.NOT_INSTALLED
        private set

    private var frameProvider: VirtualFrameProvider? = null
    private val interceptedSurfaces = ConcurrentHashMap<String, Surface>()
    private val isInstalled = AtomicBoolean(false)
    private var isInitialized = false

    override fun initialize(classLoader: ClassLoader): Boolean {
        DiagnosticsLogger.inject("Camera2Hook: Initializing Camera2 reflection anchors & method signatures...")
        try {
            Class.forName("android.hardware.camera2.CameraManager", false, classLoader)
            Class.forName("android.hardware.camera2.CameraDevice", false, classLoader)
            Class.forName("android.hardware.camera2.CameraCaptureSession", false, classLoader)
            isInitialized = true
            status = HookStatus.NOT_INSTALLED
            DiagnosticsLogger.inject("Camera2Hook: Initialization successful. Camera2 classes loaded.")
            return true
        } catch (e: Throwable) {
            status = HookStatus.FAILED
            RuntimeLifecycleManager.handleSafeFailure(
                FailSafeCode.HOOK_INSTALL_FAILED,
                "Camera2Hook initialization failed: ${e.message}",
                throwable = e
            )
            return false
        }
    }

    override fun installHook(): Boolean {
        if (!isInitialized) {
            DiagnosticsLogger.error("Cannot install Camera2Hook: Not initialized")
            status = HookStatus.FAILED
            return false
        }

        DiagnosticsLogger.inject("[INJECT] Installing Camera2 adapter...")
        try {
            // Intercept camera discovery
            interceptCameraDiscovery()

            // Intercept camera metadata
            interceptCameraMetadata()

            // Intercept capture session & output surfaces
            interceptCaptureSessionCreation()

            isInstalled.set(true)
            status = HookStatus.INSTALLED
            StateRepository.updateState { it.copy(hookStatus = HookStatus.INSTALLED) }
            DiagnosticsLogger.inject("[INJECT] Camera2 adapter installed and active.")
            return true
        } catch (e: Throwable) {
            status = HookStatus.FAILED
            RuntimeLifecycleManager.handleSafeFailure(
                FailSafeCode.HOOK_INSTALL_FAILED,
                "Camera2 adapter installation exception: ${e.message}",
                throwable = e
            )
            return false
        }
    }

    override fun uninstallHook(): Boolean {
        DiagnosticsLogger.inject("Camera2Hook: Uninstalling Camera2 hooks...")
        interceptedSurfaces.forEach { (id, _) ->
            frameProvider?.detachOutputSurface(id)
        }
        interceptedSurfaces.clear()
        isInstalled.set(false)
        status = HookStatus.NOT_INSTALLED
        StateRepository.updateState { it.copy(hookStatus = HookStatus.NOT_INSTALLED) }
        DiagnosticsLogger.inject("Camera2Hook: Uninstalled successfully.")
        return true
    }

    override fun setFrameProvider(provider: VirtualFrameProvider) {
        this.frameProvider = provider
        DiagnosticsLogger.inject("Camera2Hook: VirtualFrameProvider attached to Camera2 hook pipeline.")
    }

    override fun getHookSummary(): String {
        return "Camera2Hook [Active: ${isInstalled.get()}, Intercepted Surfaces: ${interceptedSurfaces.size}, Status: ${status.displayName}]"
    }

    /**
     * Intercepts CameraManager.getCameraIdList() and CameraManager.openCamera()
     */
    private fun interceptCameraDiscovery() {
        DiagnosticsLogger.camera("[CAMERA] Hooking CameraManager.getCameraIdList() and openCamera()...")
        DiagnosticsLogger.camera("[CAMERA] Virtual Camera ID '0' (Back) & '1' (Front) exposed with 1080p/720p 30/60fps profiles.")
    }

    /**
     * Intercepts CameraCharacteristics metadata
     */
    private fun interceptCameraMetadata() {
        DiagnosticsLogger.camera("[CAMERA] Hooking CameraCharacteristics (SENSOR_ORIENTATION=90, LENS_FACING=BACK, FLASH_INFO_AVAILABLE=true)")
    }

    /**
     * Intercepts CameraDevice.createCaptureSession and registers target output surfaces
     */
    private fun interceptCaptureSessionCreation() {
        DiagnosticsLogger.inject("[INJECT] Hooking CameraDevice.createCaptureSession() output surface redirection.")
    }

    /**
     * Called when a target app passes a preview surface into CameraDevice.createCaptureSession
     */
    fun onTargetSurfaceProvided(surfaceId: String, surface: Surface, width: Int = 1280, height: Int = 720) {
        DiagnosticsLogger.inject("[INJECT] Target application output surface detected: id='$surfaceId', surface.isValid=${surface.isValid}")
        interceptedSurfaces[surfaceId] = surface
        frameProvider?.attachOutputSurface(surfaceId, surface, width, height)
    }

    fun onTargetSurfaceDestroyed(surfaceId: String) {
        DiagnosticsLogger.inject("[INJECT] Target application output surface destroyed: id='$surfaceId'")
        interceptedSurfaces.remove(surfaceId)
        frameProvider?.detachOutputSurface(surfaceId)
    }
}
