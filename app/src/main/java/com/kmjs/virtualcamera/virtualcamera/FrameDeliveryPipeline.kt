package com.kmjs.virtualcamera.virtualcamera

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.view.Surface
import com.kmjs.virtualcamera.core.DiagnosticsLogger
import com.kmjs.virtualcamera.stream.Frame

class FrameDeliveryPipeline(
    private val surface: Surface,
    private val width: Int,
    private val height: Int
) {
    private val paint = Paint(Paint.FILTER_BITMAP_FLAG)
    private val destRect = Rect(0, 0, width, height)
    private var isReleased = false

    fun renderFrame(frame: Frame) {
        if (isReleased || !surface.isValid) return

        val bitmap = frame.bitmap ?: return
        val srcRect = Rect(0, 0, bitmap.width, bitmap.height)

        var canvas: Canvas? = null
        try {
            canvas = surface.lockCanvas(null)
            if (canvas != null) {
                canvas.drawBitmap(bitmap, srcRect, destRect, paint)
            }
        } catch (e: Throwable) {
            DiagnosticsLogger.error("FrameDeliveryPipeline surface render error: ${e.message}")
        } finally {
            if (canvas != null) {
                try {
                    surface.unlockCanvasAndPost(canvas)
                } catch (e: Throwable) {
                    DiagnosticsLogger.error("FrameDeliveryPipeline unlockCanvas error: ${e.message}")
                }
            }
        }
    }

    fun release() {
        isReleased = true
    }
}
