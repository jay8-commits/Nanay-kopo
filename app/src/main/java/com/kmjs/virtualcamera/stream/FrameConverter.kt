package com.kmjs.virtualcamera.stream

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import com.kmjs.virtualcamera.core.DiagnosticsLogger
import com.kmjs.virtualcamera.core.PixelFormat
import java.nio.ByteBuffer

object FrameConverter {

    fun convertFrame(
        srcFrame: Frame,
        targetWidth: Int,
        targetHeight: Int,
        targetFormat: PixelFormat,
        targetRotation: Int
    ): Frame {
        // Fast path: if dimensions, format, and rotation match, return as-is
        if (srcFrame.width == targetWidth &&
            srcFrame.height == targetHeight &&
            srcFrame.pixelFormat == targetFormat &&
            srcFrame.rotation == targetRotation &&
            srcFrame.bitmap != null
        ) {
            return srcFrame
        }

        var sourceBitmap = srcFrame.bitmap
        if (sourceBitmap == null && srcFrame.data != null) {
            sourceBitmap = byteBufferToBitmap(srcFrame.data, srcFrame.width, srcFrame.height, srcFrame.pixelFormat)
        }

        if (sourceBitmap == null) {
            DiagnosticsLogger.error("FrameConverter: source bitmap and data are both null for frame #${srcFrame.frameNumber}")
            return srcFrame
        }

        // Apply rotation and scaling
        val matrix = Matrix()
        val rotationDiff = (targetRotation - srcFrame.rotation + 360) % 360
        if (rotationDiff != 0) {
            matrix.postRotate(rotationDiff.toFloat())
        }

        val scaleX = targetWidth.toFloat() / sourceBitmap.width.toFloat()
        val scaleY = targetHeight.toFloat() / sourceBitmap.height.toFloat()
        matrix.postScale(scaleX, scaleY)

        val transformedBitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(transformedBitmap)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(sourceBitmap, matrix, paint)

        val targetBuffer = when (targetFormat) {
            PixelFormat.RGBA_8888 -> {
                val buffer = ByteBuffer.allocateDirect(targetWidth * targetHeight * 4)
                transformedBitmap.copyPixelsToBuffer(buffer)
                buffer.rewind()
                buffer
            }
            PixelFormat.NV21 -> {
                rgbaToNv21(transformedBitmap, targetWidth, targetHeight)
            }
            PixelFormat.YUV_420_888 -> {
                rgbaToNv21(transformedBitmap, targetWidth, targetHeight)
            }
            PixelFormat.RGB_565 -> {
                val buffer = ByteBuffer.allocateDirect(targetWidth * targetHeight * 2)
                val rgb565 = transformedBitmap.copy(Bitmap.Config.RGB_565, false)
                rgb565.copyPixelsToBuffer(buffer)
                buffer.rewind()
                buffer
            }
        }

        val converted = Frame(
            frameNumber = srcFrame.frameNumber,
            timestampNs = srcFrame.timestampNs,
            width = targetWidth,
            height = targetHeight,
            pixelFormat = targetFormat,
            rotation = targetRotation,
            data = targetBuffer,
            bitmap = transformedBitmap,
            isKeyFrame = srcFrame.isKeyFrame
        )

        DiagnosticsLogger.frame("[KMJS_FRAME_CONVERTED] frame=${srcFrame.frameNumber} size=${targetWidth}x${targetHeight} format=${targetFormat.name}")
        return converted
    }

    private fun byteBufferToBitmap(buffer: ByteBuffer, width: Int, height: Int, format: PixelFormat): Bitmap {
        buffer.rewind()
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        if (format == PixelFormat.RGBA_8888) {
            bitmap.copyPixelsFromBuffer(buffer)
        } else {
            // Software fallback for non-RGBA
            val pixels = IntArray(width * height)
            for (i in 0 until (width * height).coerceAtMost(buffer.remaining() / 4)) {
                val r = buffer.get().toInt() and 0xFF
                val g = buffer.get().toInt() and 0xFF
                val b = buffer.get().toInt() and 0xFF
                val a = buffer.get().toInt() and 0xFF
                pixels[i] = (a shl 24) or (r shl 16) or (g shl 8) or b
            }
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        }
        return bitmap
    }

    fun rgbaToNv21(bitmap: Bitmap, width: Int, height: Int): ByteBuffer {
        val argb = IntArray(width * height)
        bitmap.getPixels(argb, 0, width, 0, 0, width, height)

        val yuvSize = width * height * 3 / 2
        val yuvBuffer = ByteBuffer.allocateDirect(yuvSize)
        val yuv = ByteArray(yuvSize)

        var yIndex = 0
        var uvIndex = width * height

        for (j in 0 until height) {
            for (i in 0 until width) {
                val pixel = argb[j * width + i]
                val r = (pixel shr 16) and 0xff
                val g = (pixel shr 8) and 0xff
                val b = pixel and 0xff

                // RGB to YUV standard formula
                val y = ((66 * r + 129 * g + 25 * b + 128) shr 8) + 16
                val u = ((-38 * r - 74 * g + 112 * b + 128) shr 8) + 128
                val v = ((112 * r - 94 * g - 18 * b + 128) shr 8) + 128

                yuv[yIndex++] = y.coerceIn(0, 255).toByte()

                if (j % 2 == 0 && i % 2 == 0) {
                    yuv[uvIndex++] = v.coerceIn(0, 255).toByte() // NV21: V followed by U
                    yuv[uvIndex++] = u.coerceIn(0, 255).toByte()
                }
            }
        }
        yuvBuffer.put(yuv)
        yuvBuffer.rewind()
        return yuvBuffer
    }
}
