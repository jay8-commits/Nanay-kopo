package com.kmjs.virtualcamera.stream

import com.kmjs.virtualcamera.core.DiagnosticsLogger
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class FrameBuffer(
    val capacity: Int = 8,
    private val dropSlowFrames: Boolean = true
) {
    private val queue = ArrayBlockingQueue<Frame>(capacity)
    private val lock = ReentrantLock()
    private val _droppedFrames = AtomicLong(0)
    private val _totalPushed = AtomicLong(0)
    private val _totalPopped = AtomicLong(0)

    @Volatile
    private var lastValidFrame: Frame? = null

    val droppedCount: Long
        get() = _droppedFrames.get()

    val totalPushedCount: Long
        get() = _totalPushed.get()

    val totalPoppedCount: Long
        get() = _totalPopped.get()

    val size: Int
        get() = queue.size

    fun push(frame: Frame): Boolean {
        _totalPushed.incrementAndGet()
        lock.withLock {
            lastValidFrame = frame
            if (queue.size >= capacity) {
                if (dropSlowFrames) {
                    val dropped = queue.poll()
                    _droppedFrames.incrementAndGet()
                    if (_droppedFrames.get() % 30 == 1L) {
                        DiagnosticsLogger.frame("Buffer full (capacity=$capacity). Dropped frame #${dropped?.frameNumber ?: 0}. Total dropped=${_droppedFrames.get()}")
                    }
                } else {
                    return false
                }
            }
            return queue.offer(frame)
        }
    }

    fun pop(): Frame? {
        val frame = queue.poll()
        if (frame != null) {
            _totalPopped.incrementAndGet()
            return frame
        }
        // Consumer faster than producer: return last valid frame if available
        return lastValidFrame
    }

    fun peek(): Frame? {
        return queue.peek() ?: lastValidFrame
    }

    fun clear() {
        lock.withLock {
            queue.clear()
            lastValidFrame = null
            _droppedFrames.set(0)
            _totalPushed.set(0)
            _totalPopped.set(0)
        }
    }
}
