package com.example.vision.processing

data class FrameData(
    val width: Int,
    val height: Int,
    val y: ByteArray,
    val u: ByteArray,
    val v: ByteArray,
    val yRowStride: Int,
    val uRowStride: Int,
    val vRowStride: Int,
    val yPixelStride: Int,
    val uPixelStride: Int,
    val vPixelStride: Int
)

