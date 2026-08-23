package com.kmjs.virtualcamera.testing

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import com.kmjs.virtualcamera.core.PixelFormat
import com.kmjs.virtualcamera.stream.Frame
import com.kmjs.virtualcamera.stream.FrameConverter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PatternTestFrameProvider {

    private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

    fun generateFrame(
        width: Int,
        height: Int,
        pixelFormat: PixelFormat,
        rotation: Int,
        frameNumber: Long
    ): Frame {
        var bitmap: Bitmap? = null
        try {
            bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            // 1. Draw SMPTE Color Bars background
            val barColors = intArrayOf(
                Color.rgb(180, 180, 180), // White/Silver
                Color.rgb(220, 220, 0),   // Yellow
                Color.rgb(0, 220, 220),   // Cyan
                Color.rgb(0, 220, 0),     // Green
                Color.rgb(220, 0, 220),   // Magenta
                Color.rgb(220, 0, 0),     // Red
                Color.rgb(0, 0, 220),     // Blue
                Color.rgb(24, 24, 24)     // Dark Slate
            )

            val barWidth = width / barColors.size
            for (i in barColors.indices) {
                val paint = Paint().apply { color = barColors[i] }
                canvas.drawRect(
                    (i * barWidth).toFloat(),
                    0f,
                    ((i + 1) * barWidth).toFloat(),
                    (height * 0.70f),
                    paint
                )
            }

            // 2. Bottom lower third bar
            val bottomPaint = Paint().apply { color = Color.rgb(15, 23, 42) }
            canvas.drawRect(0f, height * 0.70f, width.toFloat(), height.toFloat(), bottomPaint)

            // 3. Animated Scan Line
            val scanX = ((frameNumber * 12) % width).toFloat()
            val scanPaint = Paint().apply {
                color = Color.rgb(56, 189, 248)
                strokeWidth = 6f
                setShadowLayer(10f, 0f, 0f, Color.CYAN)
            }
            canvas.drawLine(scanX, 0f, scanX, height.toFloat(), scanPaint)

            // 4. Center badge overlay
            val badgeWidth = (width * 0.75f).coerceAtLeast(300f)
            val badgeHeight = (height * 0.35f).coerceAtLeast(140f)
            val badgeLeft = (width - badgeWidth) / 2f
            val badgeTop = (height * 0.20f)
            val badgeBgPaint = Paint().apply {
                color = Color.argb(220, 15, 23, 42)
                style = Paint.Style.FILL
            }
            val badgeBorderPaint = Paint().apply {
                color = Color.rgb(13, 148, 136)
                style = Paint.Style.STROKE
                strokeWidth = 4f
            }
            canvas.drawRoundRect(badgeLeft, badgeTop, badgeLeft + badgeWidth, badgeTop + badgeHeight, 16f, 16f, badgeBgPaint)
            canvas.drawRoundRect(badgeLeft, badgeTop, badgeLeft + badgeWidth, badgeTop + badgeHeight, 16f, 16f, badgeBorderPaint)

            // Text paints
            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(248, 250, 252)
                textSize = (height * 0.055f).coerceIn(20f, 42f)
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(56, 189, 248)
                textSize = (height * 0.040f).coerceIn(16f, 28f)
                typeface = Typeface.MONOSPACE
                textAlign = Paint.Align.CENTER
            }
            val subTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(148, 163, 184)
                textSize = (height * 0.032f).coerceIn(13f, 22f)
                textAlign = Paint.Align.CENTER
            }

            val centerX = width / 2f
            canvas.drawText("KMJS VIRTUAL CAMERA", centerX, badgeTop + (badgeHeight * 0.32f), titlePaint)
            canvas.drawText("TEST PATTERN GENERATOR", centerX, badgeTop + (badgeHeight * 0.58f), textPaint)
            canvas.drawText("RESOLUTION: ${width}x${height} | FORMAT: ${pixelFormat.displayName}", centerX, badgeTop + (badgeHeight * 0.82f), subTextPaint)

            // 5. Lower third dynamic information (Timestamp, Frame Counter, and FPS)
            val nowFormatted = timeFormat.format(Date())
            val bottomTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(255, 255, 255)
                textSize = (height * 0.045f).coerceIn(16f, 32f)
                typeface = Typeface.MONOSPACE
            }
            canvas.drawText("TIMESTAMP: $nowFormatted", 24f, height * 0.82f, bottomTextPaint)
            canvas.drawText("FRAME: #%04d".format(frameNumber), 24f, height * 0.92f, bottomTextPaint)

            val fpsEstimate = 30
            val fpsTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(34, 197, 94) // Green OK
                textSize = (height * 0.045f).coerceIn(16f, 32f)
                typeface = Typeface.MONOSPACE
                textAlign = Paint.Align.RIGHT
            }
            canvas.drawText("FPS: %02d".format(fpsEstimate), width - 24f, height * 0.82f, fpsTextPaint)

            val rotTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(226, 232, 240)
                textSize = (height * 0.038f).coerceIn(14f, 26f)
                typeface = Typeface.MONOSPACE
                textAlign = Paint.Align.RIGHT
            }
            canvas.drawText("STATUS: ACTIVE | ROT: ${rotation}°", width - 24f, height * 0.92f, rotTextPaint)
        } catch (_: Throwable) {
            // JVM Unit testing fallback
            bitmap = null
        }

        val rawFrame = Frame(
            frameNumber = frameNumber,
            timestampNs = System.currentTimeMillis() * 1_000_000L,
            width = width,
            height = height,
            pixelFormat = PixelFormat.RGBA_8888,
            rotation = rotation,
            bitmap = bitmap
        )

        return FrameConverter.convertFrame(rawFrame, width, height, pixelFormat, rotation)
    }
}
