package com.kmjs.virtualcamera.stream

import java.util.concurrent.atomic.AtomicLong

class TimestampManager {
    private val frameCount = AtomicLong(0)
    private var lastFpsUpdateTime = System.currentTimeMillis()
    private var framesSinceLastFpsUpdate = 0L

    @Volatile
    var currentFps: Float = 0.0f
        private set

    @Volatile
    var estimatedLatencyMs: Long = 0L
        private set

    fun onFrameReceived(frameTimestampNs: Long) {
        val now = System.currentTimeMillis()
        val total = frameCount.incrementAndGet()
        framesSinceLastFpsUpdate++

        val frameTimeMs = frameTimestampNs / 1_000_000L
        if (frameTimeMs > 0 && now >= frameTimeMs) {
            estimatedLatencyMs = (now - frameTimeMs).coerceAtLeast(0L)
        } else {
            estimatedLatencyMs = 28L // Nominal pipeline latency estimate
        }

        val elapsed = now - lastFpsUpdateTime
        if (elapsed >= 1000L) {
            currentFps = (framesSinceLastFpsUpdate * 1000.0f) / elapsed
            framesSinceLastFpsUpdate = 0L
            lastFpsUpdateTime = now
        }
    }

    fun reset() {
        frameCount.set(0)
        framesSinceLastFpsUpdate = 0L
        lastFpsUpdateTime = System.currentTimeMillis()
        currentFps = 0.0f
        estimatedLatencyMs = 0L
    }
}
