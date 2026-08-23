package com.kmjs.virtualcamera.virtualcamera

import android.view.Surface
import com.kmjs.virtualcamera.core.DiagnosticsLogger
import com.kmjs.virtualcamera.core.PixelFormat
import com.kmjs.virtualcamera.core.StateRepository
import com.kmjs.virtualcamera.stream.Frame
import com.kmjs.virtualcamera.stream.FrameConverter
import com.kmjs.virtualcamera.stream.RtspDecoder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

interface VirtualFrameProvider {
    fun getLatestFrame(): Frame?
    fun getFormattedFrame(width: Int, height: Int, format: PixelFormat, rotation: Int): Frame?
    fun registerConsumer(consumerId: String, onFrameReady: (Frame) -> Unit)
    fun unregisterConsumer(consumerId: String)
    fun getDeliveredFrameCount(): Long
    fun attachOutputSurface(surfaceId: String, surface: Surface, width: Int, height: Int)
    fun detachOutputSurface(surfaceId: String)
}

class VirtualFrameProviderImpl(
    private val decoder: RtspDecoder
) : VirtualFrameProvider {

    private val deliveredCount = AtomicLong(0)
    private val consumers = ConcurrentHashMap<String, (Frame) -> Unit>()
    private val activeSurfaces = ConcurrentHashMap<String, FrameDeliveryPipeline>()
    private val scope = CoroutineScope(Dispatchers.Default)
    private var deliveryJob: Job? = null
    private val isRunning = AtomicBoolean(false)

    fun startDelivery() {
        if (isRunning.get()) return
        isRunning.set(true)
        DiagnosticsLogger.inject("[TARGET_FRAME_PROVIDER_CONNECTED] VirtualFrameProvider delivery loop started (Consumers: ${consumers.size}, Surfaces: ${activeSurfaces.size})")

        deliveryJob = scope.launch {
            while (isRunning.get() && isActive) {
                val frame = decoder.getFrame()
                if (frame != null) {
                    val count = deliveredCount.incrementAndGet()
                    val fps = 30
                    DiagnosticsLogger.frame("[TARGET_FRAME_DELIVERED] frame=$count size=${frame.width}x${frame.height} format=${frame.pixelFormat.name} fps=$fps")

                    // Dispatch to registered callbacks
                    consumers.values.forEach { callback ->
                        try {
                            callback(frame)
                        } catch (t: Throwable) {
                            DiagnosticsLogger.error("Consumer frame dispatch error", throwable = t)
                        }
                    }

                    // Deliver to active hardware surfaces
                    activeSurfaces.values.forEach { pipeline ->
                        pipeline.renderFrame(frame)
                    }

                    // Update UI state
                    if (count % 10 == 0L) {
                        StateRepository.updateState {
                            it.copy(framesDelivered = count)
                        }
                    }
                }
                delay(16L) // ~60fps poll
            }
        }
    }

    fun stopDelivery() {
        isRunning.set(false)
        deliveryJob?.cancel()
        deliveryJob = null
        activeSurfaces.values.forEach { it.release() }
        activeSurfaces.clear()
        DiagnosticsLogger.inject("VirtualFrameProvider delivery loop stopped")
    }

    override fun getLatestFrame(): Frame? {
        return decoder.getFrame()
    }

    override fun getFormattedFrame(width: Int, height: Int, format: PixelFormat, rotation: Int): Frame? {
        val raw = decoder.getFrame() ?: return null
        return FrameConverter.convertFrame(raw, width, height, format, rotation)
    }

    override fun registerConsumer(consumerId: String, onFrameReady: (Frame) -> Unit) {
        consumers[consumerId] = onFrameReady
        DiagnosticsLogger.inject("[TARGET_FRAME_PROVIDER_CONNECTED] Consumer registered: $consumerId (Total consumers: ${consumers.size})")
    }

    override fun unregisterConsumer(consumerId: String) {
        consumers.remove(consumerId)
        DiagnosticsLogger.inject("Consumer unregistered: $consumerId")
    }

    override fun getDeliveredFrameCount(): Long {
        return deliveredCount.get()
    }

    override fun attachOutputSurface(surfaceId: String, surface: Surface, width: Int, height: Int) {
        val pipeline = FrameDeliveryPipeline(surface, width, height)
        activeSurfaces[surfaceId] = pipeline
        DiagnosticsLogger.inject("[TARGET_FRAME_PROVIDER_CONNECTED] Attached output surface [$surfaceId] (${width}x${height})")
    }

    override fun detachOutputSurface(surfaceId: String) {
        activeSurfaces.remove(surfaceId)?.release()
        DiagnosticsLogger.inject("Detached output surface [$surfaceId]")
    }
}
