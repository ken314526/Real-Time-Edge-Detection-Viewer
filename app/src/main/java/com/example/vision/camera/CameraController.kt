package com.example.vision.camera

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.media.Image
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.util.Size
import android.view.Surface
import android.view.TextureView
import androidx.core.content.getSystemService
import com.example.vision.processing.FrameData
import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

class CameraController(
    private val context: Context,
    private val textureView: TextureView,
    private val onFrameAvailable: (FrameData) -> Unit,
    private val onResolutionUpdate: (Int, Int) -> Unit,
    private val onFpsUpdate: (Double) -> Unit
) {

    private val cameraManager = context.getSystemService<CameraManager>()!!
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var imageReader: ImageReader? = null
    private var backgroundHandler: Handler? = null
    private var backgroundThread: HandlerThread? = null

    private var lastFrameTimestampNs: Long = 0
    private var fpsAccumulator = 0.0
    private var frameCount = 0

    fun start() {
        if (!textureView.isAvailable) {
            textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                override fun onSurfaceTextureAvailable(surfaceTexture: android.graphics.SurfaceTexture, width: Int, height: Int) {
                    openCamera()
                }

                override fun onSurfaceTextureSizeChanged(surfaceTexture: android.graphics.SurfaceTexture, width: Int, height: Int) = Unit
                override fun onSurfaceTextureDestroyed(surfaceTexture: android.graphics.SurfaceTexture): Boolean = true
                override fun onSurfaceTextureUpdated(surfaceTexture: android.graphics.SurfaceTexture) = Unit
            }
        } else {
            openCamera()
        }
    }

    fun resume() {
        if (cameraDevice == null) {
            start()
        }
    }

    fun pause() {
        captureSession?.close()
        captureSession = null
        cameraDevice?.close()
        cameraDevice = null
        stopBackgroundThread()
    }

    fun release() {
        pause()
        imageReader?.close()
        imageReader = null
    }

    @SuppressLint("MissingPermission")
    private fun openCamera() {
        startBackgroundThread()
        val cameraId = selectBackCameraId() ?: return
        cameraManager.openCamera(cameraId, stateCallback, backgroundHandler)
    }

    private fun selectBackCameraId(): String? {
        for (cameraId in cameraManager.cameraIdList) {
            val characteristics = cameraManager.getCameraCharacteristics(cameraId)
            val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
            if (facing == CameraCharacteristics.LENS_FACING_BACK) {
                return cameraId
            }
        }
        return cameraManager.cameraIdList.firstOrNull()
    }

    private val stateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(device: CameraDevice) {
            cameraDevice = device
            createPreviewSession()
        }

        override fun onDisconnected(device: CameraDevice) {
            device.close()
            cameraDevice = null
        }

        override fun onError(device: CameraDevice, error: Int) {
            device.close()
            cameraDevice = null
        }
    }

    private fun createPreviewSession() {
        val device = cameraDevice ?: return
        val optimalSize = selectOptimalSize(device.id)
        val surfaceTexture = textureView.surfaceTexture ?: return
        surfaceTexture.setDefaultBufferSize(optimalSize.width, optimalSize.height)
        val previewSurface = Surface(surfaceTexture)

        imageReader?.close()
        imageReader = ImageReader.newInstance(
            optimalSize.width,
            optimalSize.height,
            ImageFormat.YUV_420_888,
            3
        ).apply {
            setOnImageAvailableListener({ reader ->
                reader.acquireLatestImage()?.use { image ->
                    handleImage(image)
                }
            }, backgroundHandler)
        }

        val surfaces = listOf(previewSurface, imageReader!!.surface)

        device.createCaptureSession(
            surfaces,
            object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    captureSession = session
                    val requestBuilder = device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                        addTarget(previewSurface)
                        addTarget(imageReader!!.surface)
                        set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
                    }
                    session.setRepeatingRequest(requestBuilder.build(), captureCallback, backgroundHandler)
                    onResolutionUpdate(optimalSize.width, optimalSize.height)
                }

                override fun onConfigureFailed(session: CameraCaptureSession) = Unit
            },
            backgroundHandler
        )
    }

    private val captureCallback = object : CameraCaptureSession.CaptureCallback() {
        override fun onCaptureCompleted(session: CameraCaptureSession, request: CaptureRequest, result: TotalCaptureResult) {
            super.onCaptureCompleted(session, request, result)
            val timestamp = result.get(CaptureResult.SENSOR_TIMESTAMP) ?: return
            if (lastFrameTimestampNs != 0L) {
                val delta = timestamp - lastFrameTimestampNs
                if (delta > 0) {
                    val fpsInstant = TimeUnit.SECONDS.toNanos(1).toDouble() / delta.toDouble()
                    fpsAccumulator += fpsInstant
                    frameCount++
                    if (frameCount >= 10) {
                        onFpsUpdate(fpsAccumulator / frameCount)
                        fpsAccumulator = 0.0
                        frameCount = 0
                    }
                }
            }
            lastFrameTimestampNs = timestamp
        }
    }

    private fun handleImage(image: Image) {
        val width = image.width
        val height = image.height

        val yPlane = image.planes[0]
        val uPlane = image.planes[1]
        val vPlane = image.planes[2]

        val yBuffer = yPlane.buffer.toByteArray()
        val uBuffer = uPlane.buffer.toByteArray()
        val vBuffer = vPlane.buffer.toByteArray()

        val frameData = FrameData(
            width = width,
            height = height,
            y = yBuffer,
            u = uBuffer,
            v = vBuffer,
            yRowStride = yPlane.rowStride,
            uRowStride = uPlane.rowStride,
            vRowStride = vPlane.rowStride,
            yPixelStride = yPlane.pixelStride,
            uPixelStride = uPlane.pixelStride,
            vPixelStride = vPlane.pixelStride
        )
        onFrameAvailable(frameData)
    }

    private fun ByteBuffer.toByteArray(): ByteArray {
        val remaining = remaining()
        val array = ByteArray(remaining)
        get(array)
        rewind()
        return array
    }

    private fun selectOptimalSize(cameraId: String): Size {
        val characteristics = cameraManager.getCameraCharacteristics(cameraId)
        val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            ?: return Size(1280, 720)
        val sizes = map.getOutputSizes(ImageFormat.YUV_420_888)
            ?: return Size(1280, 720)
        val targetArea = 1280 * 720
        return sizes.minByOrNull { size ->
            val difference = (size.width * size.height) - targetArea
            kotlin.math.abs(difference)
        } ?: sizes.first()
    }

    private fun startBackgroundThread() {
        if (backgroundThread == null) {
            backgroundThread = HandlerThread("CameraBackground").also {
                it.start()
                backgroundHandler = Handler(it.looper)
            }
        }
    }

    private fun stopBackgroundThread() {
        backgroundThread?.quitSafely()
        try {
            backgroundThread?.join()
        } catch (_: InterruptedException) {
        } finally {
            backgroundThread = null
            backgroundHandler = null
        }
    }
}

