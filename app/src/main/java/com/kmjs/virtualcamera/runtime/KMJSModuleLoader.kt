package com.kmjs.virtualcamera.runtime

import com.kmjs.virtualcamera.camera.Camera2Hook
import com.kmjs.virtualcamera.camera.CameraHook
import com.kmjs.virtualcamera.camera.CameraXHook
import com.kmjs.virtualcamera.camera.LegacyCameraHook
import com.kmjs.virtualcamera.core.CameraApiType
import com.kmjs.virtualcamera.core.DecoderStatus
import com.kmjs.virtualcamera.core.DiagnosticsLogger
import com.kmjs.virtualcamera.core.FailSafeCode
import com.kmjs.virtualcamera.core.HookStatus
import com.kmjs.virtualcamera.core.RuntimeLifecycleManager
import com.kmjs.virtualcamera.core.StateRepository
import com.kmjs.virtualcamera.core.StreamStatus
import com.kmjs.virtualcamera.core.VirtualCameraConfig
import com.kmjs.virtualcamera.stream.FrameBuffer
import com.kmjs.virtualcamera.stream.RtspDecoder
import com.kmjs.virtualcamera.virtualcamera.VirtualFrameProvider
import com.kmjs.virtualcamera.virtualcamera.VirtualFrameProviderImpl
import java.util.concurrent.atomic.AtomicBoolean

object KMJSModuleLoader {

    private val isLoaded = AtomicBoolean(false)
    private var activeCameraHook: CameraHook? = null
    private var rtspDecoder: RtspDecoder? = null
    private var virtualFrameProvider: VirtualFrameProviderImpl? = null

    val isInitialized: Boolean
        get() = isLoaded.get()

    /**
     * Executes the mandatory 10-step startup sequence for KMJS Virtual Camera.
     */
    fun startModule(classLoader: ClassLoader = javaClass.classLoader!!): Boolean {
        // Step 1: Initialize runtime
        DiagnosticsLogger.module("KMJS module initialized (Version: 1.0.0, NPatch Runtime)")
        RuntimeLifecycleManager.startMonitoring()

        // Step 2 & 3: Detect package & process
        val processInfo = TargetProcessDetector.detectCurrentProcess(classLoader)
        StateRepository.updateState {
            it.copy(
                detectedPackage = processInfo.packageName,
                detectedProcess = processInfo.processName,
                isConnected = true
            )
        }

        if (processInfo.isAuxiliaryProcess) {
            DiagnosticsLogger.module("Exiting hook installation for auxiliary process (${processInfo.processName})")
            return false
        }

        // Step 4: Check target registry
        StateRepository.updateState { it.copy(targetSupported = processInfo.isSupported) }
        if (!processInfo.isSupported) {
            RuntimeLifecycleManager.handleSafeFailure(
                FailSafeCode.TARGET_UNSUPPORTED,
                "Package '${processInfo.packageName}' is not in SupportedTargetRegistry or disabled",
                processInfo.processName
            )
            return false
        }

        // Step 5: Detect camera API
        val apiDetection = CameraApiDetector.detectCameraApi(classLoader)
        StateRepository.updateState { it.copy(cameraApi = apiDetection.primaryApi) }

        if (apiDetection.primaryApi == CameraApiType.UNKNOWN) {
            RuntimeLifecycleManager.handleSafeFailure(
                FailSafeCode.CAMERA_API_UNSUPPORTED,
                "No supported camera API found in target process",
                processInfo.processName
            )
            return false
        }

        // Step 6: Select adapter
        activeCameraHook = when (apiDetection.primaryApi) {
            CameraApiType.CAMERA2 -> Camera2Hook()
            CameraApiType.CAMERAX -> CameraXHook()
            CameraApiType.LEGACY -> LegacyCameraHook()
            CameraApiType.UNKNOWN -> null
        }

        if (activeCameraHook == null) {
            RuntimeLifecycleManager.handleSafeFailure(
                FailSafeCode.CAMERA_API_UNSUPPORTED,
                "Unable to create adapter for ${apiDetection.primaryApi.displayName}",
                processInfo.processName
            )
            return false
        }

        // Step 7: Initialize diagnostics & decoder/pipeline
        val frameBuffer = FrameBuffer(capacity = 8, dropSlowFrames = true)
        val decoder = RtspDecoder(frameBuffer)
        val frameProvider = VirtualFrameProviderImpl(decoder)
        rtspDecoder = decoder
        virtualFrameProvider = frameProvider

        // Step 8: Install hook
        activeCameraHook?.let { hook ->
            hook.initialize(classLoader)
            hook.setFrameProvider(frameProvider)
            val installSuccess = hook.installHook()
            if (!installSuccess) {
                RuntimeLifecycleManager.handleSafeFailure(
                    FailSafeCode.HOOK_INSTALL_FAILED,
                    "Camera adapter installation failed",
                    processInfo.processName
                )
                return false
            }
        }

        // Step 9: Start stream when requested (or auto-start test pattern)
        val config = StateRepository.config.value
        decoder.config = config
        decoder.connect(config.rtspUrl, config.username, config.password)
        decoder.start()

        // Step 10: Deliver frames
        frameProvider.startDelivery()

        isLoaded.set(true)
        StateRepository.updateState {
            it.copy(
                isConnected = true,
                hookStatus = HookStatus.INSTALLED,
                streamStatus = StreamStatus.CONNECTED,
                decoderStatus = DecoderStatus.RUNNING
            )
        }

        DiagnosticsLogger.module("KMJS 10-step startup sequence completed successfully. Virtual camera active.")
        return true
    }

    fun stopModule() {
        DiagnosticsLogger.module("Stopping KMJS module runtime...")
        virtualFrameProvider?.stopDelivery()
        rtspDecoder?.stop()
        rtspDecoder?.disconnect()
        activeCameraHook?.uninstallHook()
        isLoaded.set(false)
        StateRepository.updateState {
            it.copy(
                isConnected = false,
                hookStatus = HookStatus.NOT_INSTALLED,
                streamStatus = StreamStatus.DISCONNECTED,
                decoderStatus = DecoderStatus.STOPPED
            )
        }
        RuntimeLifecycleManager.stopMonitoring()
        DiagnosticsLogger.module("KMJS module stopped.")
    }

    fun getActiveHook(): CameraHook? = activeCameraHook
    fun getDecoder(): RtspDecoder? = rtspDecoder
    fun getFrameProvider(): VirtualFrameProvider? = virtualFrameProvider
}
