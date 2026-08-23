package com.kmjs.virtualcamera

import com.kmjs.virtualcamera.core.CameraApiType
import com.kmjs.virtualcamera.core.DiagnosticsLogger
import com.kmjs.virtualcamera.core.LogCategory
import com.kmjs.virtualcamera.core.PixelFormat
import com.kmjs.virtualcamera.core.VirtualCameraConfig
import com.kmjs.virtualcamera.runtime.CameraApiDetector
import com.kmjs.virtualcamera.runtime.SupportedTargetRegistry
import com.kmjs.virtualcamera.runtime.TargetDefinition
import com.kmjs.virtualcamera.stream.Frame
import com.kmjs.virtualcamera.stream.FrameBuffer
import com.kmjs.virtualcamera.stream.TimestampManager
import com.kmjs.virtualcamera.testing.DiagnosticTestRunner
import com.kmjs.virtualcamera.testing.TestStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class KMJSVirtualCameraUnitTest {

    @Before
    fun setup() {
        DiagnosticsLogger.clear()
        SupportedTargetRegistry.resetToDefaults()
    }

    @Test
    fun testInitialTargetRegistryContainsPhotoCamera() {
        val target = SupportedTargetRegistry.find("com.photo.android.camera")
        assertNotNull("com.photo.android.camera must be pre-configured", target)
        assertEquals("Camera2Hook", target?.adapterType)
        assertTrue(target?.enabled == true)
    }

    @Test
    fun testTargetRegistryDynamicAddAndToggle() {
        val newTarget = TargetDefinition(
            packageName = "com.custom.app",
            processNameFilter = "com.custom.app",
            supportedApis = listOf(CameraApiType.CAMERA2),
            adapterType = "Camera2Hook",
            enabled = true,
            compatibilityNotes = "Custom test app"
        )
        SupportedTargetRegistry.register(newTarget)
        assertTrue(SupportedTargetRegistry.isSupported("com.custom.app"))

        SupportedTargetRegistry.setEnabled("com.custom.app", false)
        assertFalse(SupportedTargetRegistry.isSupported("com.custom.app"))

        SupportedTargetRegistry.unregister("com.custom.app")
        assertFalse(SupportedTargetRegistry.isSupported("com.custom.app"))
    }

    @Test
    fun testFrameBufferBoundedCapacityAndDrop() {
        val buffer = FrameBuffer(capacity = 3, dropSlowFrames = true)
        assertEquals(0, buffer.size)

        val frame1 = Frame(1L, 1000L, 640, 480, PixelFormat.RGBA_8888, 0)
        val frame2 = Frame(2L, 2000L, 640, 480, PixelFormat.RGBA_8888, 0)
        val frame3 = Frame(3L, 3000L, 640, 480, PixelFormat.RGBA_8888, 0)
        val frame4 = Frame(4L, 4000L, 640, 480, PixelFormat.RGBA_8888, 0)

        buffer.push(frame1)
        buffer.push(frame2)
        buffer.push(frame3)
        buffer.push(frame4) // Should drop frame1

        assertEquals(3, buffer.size)
        assertEquals(1L, buffer.droppedCount)

        val popped = buffer.pop()
        assertNotNull(popped)
        assertEquals(2L, popped?.frameNumber)
    }

    @Test
    fun testTimestampManagerFpsCalculation() {
        val manager = TimestampManager()
        for (i in 1..30) {
            manager.onFrameReceived(System.currentTimeMillis() * 1_000_000L)
        }
        assertTrue(manager.estimatedLatencyMs >= 0)
    }

    @Test
    fun testDiagnosticsLoggerCategorizationAndExport() {
        DiagnosticsLogger.clear()
        DiagnosticsLogger.module("Module boot message")
        DiagnosticsLogger.rtsp("RTSP test stream message")
        DiagnosticsLogger.error("Test error message")

        val logs = DiagnosticsLogger.logsFlow.value
        assertTrue(logs.size >= 3)
        assertTrue(logs.any { it.category == LogCategory.MODULE })
        assertTrue(logs.any { it.category == LogCategory.RTSP })
        assertTrue(logs.any { it.category == LogCategory.ERROR })

        val exported = DiagnosticsLogger.exportAllLogs()
        assertTrue(exported.contains("MODULE"))
        assertTrue(exported.contains("RTSP"))
        assertTrue(exported.contains("ERROR"))
    }

    @Test
    fun testDefaultConfigValues() {
        val config = VirtualCameraConfig()
        assertEquals("rtsp://9627b0bf2a7b.entrypoint.cloud.wowza.com:1935/app-p5260J38/66abe4b9_stream1", config.rtspUrl)
        assertEquals(1280, config.width)
        assertEquals(720, config.height)
        assertEquals(30, config.fps)
        assertEquals(0, config.rotation)
        assertEquals(PixelFormat.RGBA_8888, config.pixelFormat)
    }

    @Test
    fun testDiagnosticTestRunnerExecution() {
        DiagnosticTestRunner.runAllTests()
        val items = DiagnosticTestRunner.testItemsFlow.value
        assertEquals(13, items.size)
    }

    @Test
    fun testNPatchWorkflowRepackageAndTargetProcess() {
        val repackageResult = com.kmjs.virtualcamera.npatch.NPatchWorkflowEngine.buildPatchedTargetPackage("com.photo.android.camera")
        assertTrue(repackageResult.isSuccess)
        assertEquals("com.photo.android.camera", repackageResult.targetPackage)
        assertEquals(5, repackageResult.steps.size)

        val injectionResult = com.kmjs.virtualcamera.npatch.NPatchWorkflowEngine.executeTargetProcessPipeline(
            targetPackage = "com.photo.android.camera",
            targetPid = 19284
        )
        assertTrue(injectionResult.isSuccess)
        assertTrue(injectionResult.isHookActive)
        assertTrue(injectionResult.isFrameDelivered)
    }
}
