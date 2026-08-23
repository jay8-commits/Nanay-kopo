package com.kmjs.virtualcamera.stream

import android.graphics.Bitmap
import com.kmjs.virtualcamera.core.PixelFormat
import java.nio.ByteBuffer

data class Frame(
    val frameNumber: Long,
    val timestampNs: Long,
    val width: Int,
    val height: Int,
    val pixelFormat: PixelFormat,
    val rotation: Int, // 0, 90, 180, 270
    val data: ByteBuffer? = null,
    val bitmap: Bitmap? = null,
    val isKeyFrame: Boolean = false
) {
    val timestampMs: Long
        get() = timestampNs / 1_000_000L

    val sizeBytes: Int
        get() = (width * height * pixelFormat.bytesPerPixel).toInt()
}
