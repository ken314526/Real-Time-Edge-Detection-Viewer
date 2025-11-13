package com.example.vision.processing

object NativeProcessor {
    init {
        System.loadLibrary("visionprocessor")
    }

    external fun init(width: Int, height: Int)

    external fun processFrame(
        yPlane: ByteArray,
        uPlane: ByteArray,
        vPlane: ByteArray,
        width: Int,
        height: Int,
        yRowStride: Int,
        uRowStride: Int,
        vRowStride: Int,
        yPixelStride: Int,
        uPixelStride: Int,
        vPixelStride: Int
    ): ByteArray

    external fun release()
}

