package com.example.vision

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.vision.camera.CameraController
import com.example.vision.databinding.ActivityMainBinding
import com.example.vision.gl.ProcessedFrameRenderer
import com.example.vision.processing.FrameProcessor

class MainActivity : ComponentActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var cameraController: CameraController
    private lateinit var renderer: ProcessedFrameRenderer
    private lateinit var frameProcessor: FrameProcessor

    private var latestResolution: String = ""
    private var latestFps: String = ""

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                startCamera()
            } else {
                binding.frameStats.text = getString(R.string.permission_rationale)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        renderer = ProcessedFrameRenderer()
        binding.glSurface.setEGLContextClientVersion(2)
        binding.glSurface.setRenderer(renderer)
        binding.glSurface.renderMode = android.opengl.GLSurfaceView.RENDERMODE_CONTINUOUSLY

        frameProcessor = FrameProcessor(renderer::submitFrame)

        cameraController = CameraController(
            context = this,
            textureView = binding.cameraTexture,
            onFrameAvailable = { frame ->
                frameProcessor.process(frame)
            },
            onResolutionUpdate = { width, height ->
                frameProcessor.onResolutionChanged(width, height)
                updateResolution(width, height)
            },
            onFpsUpdate = { fps ->
                updateFps(fps)
            }
        )
    }

    override fun onStart() {
        super.onStart()
        requestCameraPermissionIfNeeded()
    }

    override fun onResume() {
        super.onResume()
        binding.glSurface.onResume()
        cameraController.resume()
    }

    override fun onPause() {
        cameraController.pause()
        binding.glSurface.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        cameraController.release()
        frameProcessor.close()
        super.onDestroy()
    }

    private fun updateFps(fps: Double) {
        latestFps = getString(R.string.fps_label, fps)
        binding.frameStats.post {
            binding.frameStats.text = listOfNotNull(latestResolution.takeIf { it.isNotEmpty() }, latestFps).joinToString(" | ")
        }
    }

    private fun updateResolution(width: Int, height: Int) {
        latestResolution = getString(R.string.resolution_label, width, height)
        binding.frameStats.post {
            binding.frameStats.text = listOfNotNull(latestResolution.takeIf { it.isNotEmpty() }, latestFps.takeIf { it.isNotEmpty() }).joinToString(" | ")
        }
    }

    private fun requestCameraPermissionIfNeeded() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> startCamera()

            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                binding.frameStats.text = getString(R.string.permission_rationale)
                permissionLauncher.launch(Manifest.permission.CAMERA)
            }

            else -> permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCamera() {
        binding.cameraTexture.post {
            cameraController.start()
        }
    }
}

