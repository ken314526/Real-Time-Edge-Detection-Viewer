package com.example.vision.processing

import java.nio.ByteBuffer

data class ProcessedFrame(
    val width: Int,
    val height: Int,
    val rgba: ByteBuffer,
    val processingTimeMs: Double
)

