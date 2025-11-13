package com.example.vision.processing

import android.os.SystemClock
import java.nio.ByteBuffer
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class FrameProcessor(
    private val onFrameProcessed: (ProcessedFrame) -> Unit
) : AutoCloseable {

    private val executor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "FrameProcessor").apply { priority = Thread.NORM_PRIORITY }
    }

    private val initialized = AtomicBoolean(false)
    private var latestWidth = 0
    private var latestHeight = 0

    fun onResolutionChanged(width: Int, height: Int) {
        latestWidth = width
        latestHeight = height
        executor.execute {
            NativeProcessor.init(width, height)
            initialized.set(true)
        }
    }

    fun process(frameData: FrameData) {
        if (!initialized.get()) return
        executor.execute {
            val startNs = System.nanoTime()
            val rgbaBytes = NativeProcessor.processFrame(
                frameData.y,
                frameData.u,
                frameData.v,
                frameData.width,
                frameData.height,
                frameData.yRowStride,
                frameData.uRowStride,
                frameData.vRowStride,
                frameData.yPixelStride,
                frameData.uPixelStride,
                frameData.vPixelStride
            )
            val buffer = ByteBuffer.allocateDirect(rgbaBytes.size).order(java.nio.ByteOrder.nativeOrder()).apply {
                put(rgbaBytes)
                position(0)
            }
            val durationMs = (System.nanoTime() - startNs) / 1_000_000.0
            onFrameProcessed(
                ProcessedFrame(
                    width = frameData.width,
                    height = frameData.height,
                    rgba = buffer,
                    processingTimeMs = durationMs
                )
            )
        }
    }

    override fun close() {
        executor.execute {
            NativeProcessor.release()
            initialized.set(false)
        }
        executor.shutdown()
    }
}

