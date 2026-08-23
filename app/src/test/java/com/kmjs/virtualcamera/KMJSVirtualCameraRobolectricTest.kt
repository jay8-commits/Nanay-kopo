package com.kmjs.virtualcamera

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.kmjs.virtualcamera.core.CameraApiType
import com.kmjs.virtualcamera.core.PixelFormat
import com.kmjs.virtualcamera.runtime.CameraApiDetector
import com.kmjs.virtualcamera.runtime.KMJSModuleLoader
import com.kmjs.virtualcamera.runtime.SupportedTargetRegistry
import com.kmjs.virtualcamera.runtime.TargetProcessDetector
import com.kmjs.virtualcamera.testing.PatternTestFrameProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class KMJSVirtualCameraRobolectricTest {

    @Test
    fun testContextAppNameResource() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("KMJS Virtual Camera", appName)
    }

    @Test
    fun testPatternTestFrameGeneration() {
        val frame = PatternTestFrameProvider.generateFrame(
            width = 1280,
            height = 720,
            pixelFormat = PixelFormat.RGBA_8888,
            rotation = 0,
            frameNumber = 42L
        )

        assertNotNull(frame.bitmap)
        assertEquals(1280, frame.width)
        assertEquals(720, frame.height)
        assertEquals(42L, frame.frameNumber)
        assertEquals(PixelFormat.RGBA_8888, frame.pixelFormat)
    }

    @Test
    fun testCameraApiDetectionInRobolectricEnvironment() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val detection = CameraApiDetector.detectCameraApi(context.classLoader)
        assertTrue(detection.hasCamera2)
        assertEquals(CameraApiType.CAMERA2, detection.primaryApi)
    }
}
