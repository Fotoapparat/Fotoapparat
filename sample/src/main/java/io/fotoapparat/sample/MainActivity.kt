package io.fotoapparat.sample

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.CompoundButton
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import io.fotoapparat.Fotoapparat
import io.fotoapparat.configuration.CameraConfiguration
import io.fotoapparat.configuration.UpdateConfiguration
import io.fotoapparat.log.logcat
import io.fotoapparat.parameter.Flash
import io.fotoapparat.parameter.Zoom
import io.fotoapparat.result.transformer.scaled
import io.fotoapparat.selector.*
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity() {

    private val permissionsDelegate = PermissionsDelegate(this)

    private var permissionsGranted: Boolean = false
    private var activeCamera: Camera = Camera.Back

    private lateinit var fotoapparat: Fotoapparat
    private lateinit var cameraZoom: Zoom.VariableZoom

    private var curZoom: Float = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        permissionsGranted = permissionsDelegate.hasCameraPermission()

        if (permissionsGranted) {
            cameraView.visibility = View.VISIBLE
        } else {
            permissionsDelegate.requestCameraPermission()
        }

        fotoapparat = Fotoapparat(
                context = this,
                view = cameraView,
                focusView = focusView,
                logger = logcat(),
                lensPosition = activeCamera.lensPosition,
                cameraConfiguration = activeCamera.configuration,
                cameraErrorCallback = { Log.e(LOGGING_TAG, "Camera error: ", it) }
        )

        capture onClick takePicture()
        switchCamera onClick changeCamera()
        torchSwitch onCheckedChanged toggleFlash()
    }

    private fun takePicture(): () -> Unit = {
        val photoResult = fotoapparat
                .autoFocus()
                .takePicture()

        photoResult
                .saveToFile(File(
                        getExternalFilesDir("photos"),
                        "photo.jpg"
                ))

        photoResult
                .toBitmap(scaled(scaleFactor = 0.25f))
                .whenAvailable { photo ->
                    photo
                            ?.let {
                                Log.i(LOGGING_TAG, "New photo captured. Bitmap length: ${it.bitmap.byteCount}")

                                val imageView = findViewById<ImageView>(R.id.result)

                                imageView.setImageBitmap(it.bitmap)
                                imageView.rotation = (-it.rotationDegrees).toFloat()
                            }
                            ?: Log.e(LOGGING_TAG, "Couldn't capture photo.")
                }
    }

    private fun changeCamera(): () -> Unit = {
        activeCamera = when (activeCamera) {
            Camera.Front -> Camera.Back
            Camera.Back -> Camera.Front
        }

        fotoapparat.switchTo(
                lensPosition = activeCamera.lensPosition,
                cameraConfiguration = activeCamera.configuration
        )

        adjustViewsVisibility()

        torchSwitch.isChecked = false

        Log.i(LOGGING_TAG, "New camera position: ${if (activeCamera is Camera.Back) "back" else "front"}")
    }

    private fun toggleFlash(): (CompoundButton, Boolean) -> Unit = { _, isChecked ->
        fotoapparat.updateConfiguration(
                UpdateConfiguration(
                        flashMode = if (isChecked) {
                            firstAvailable(
                                    torch(),
                                    off()
                            )
                        } else {
                            off()
                        }
                )
        )

        Log.i(LOGGING_TAG, "Flash is now ${if (isChecked) "on" else "off"}")
    }

    override fun onStart() {
        super.onStart()
        if (permissionsGranted) {
            fotoapparat.start()
            adjustViewsVisibility()
        }
    }

    override fun onStop() {
        super.onStop()
        if (permissionsGranted) {
            fotoapparat.stop()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (permissionsDelegate.resultGranted(requestCode, permissions, grantResults)) {
            permissionsGranted = true
            fotoapparat.start()
            adjustViewsVisibility()
            cameraView.visibility = View.VISIBLE
        }
    }

    private fun adjustViewsVisibility() {
        fotoapparat.getCapabilities()
                .whenAvailable { capabilities ->
                    capabilities
                            ?.let {
                                (it.zoom as? Zoom.VariableZoom)
                                        ?.let {
                                            cameraZoom = it
                                            focusView.scaleListener = this::scaleZoom
                                            focusView.ptrListener = this::pointerChanged
                                        }
                                        ?: run {
                                            zoomLvl?.visibility = View.GONE
                                            focusView.scaleListener = null
                                            focusView.ptrListener = null
                                        }

                                torchSwitch.visibility = if (it.flashModes.contains(Flash.Torch)) View.VISIBLE else View.GONE
                            }
                            ?: Log.e(LOGGING_TAG, "Couldn't obtain capabilities.")
                }

        switchCamera.visibility = if (fotoapparat.isAvailable(front())) View.VISIBLE else View.GONE
    }

    //When zooming slowly, the values are approximately 0.9 ~ 1.1
    private fun scaleZoom(scaleFactor: Float) {
        //convert to -0.1 ~ 0.1
        val plusZoom = if (scaleFactor < 1) -1 * (1 - scaleFactor) else scaleFactor - 1
        val newZoom = curZoom + plusZoom
        if (newZoom < 0 || newZoom > 1) return

        curZoom = newZoom
        fotoapparat.setZoom(curZoom)
        val progress = (cameraZoom.maxZoom * curZoom).roundToInt()
        val value = cameraZoom.zoomRatios[progress]
        val roundedValue = ((value.toFloat()) / 10).roundToInt().toFloat() / 10

        zoomLvl.visibility = View.VISIBLE
        zoomLvl.text = String.format("%.1f×", roundedValue)
    }

    private fun pointerChanged(fingerCount: Int){
        if(fingerCount == 0) {
            zoomLvl?.visibility = View.GONE
        }
    }
}

private const val LOGGING_TAG = "Fotoapparat Example"

private sealed class Camera(
        val lensPosition: LensPositionSelector,
        val configuration: CameraConfiguration
) {

    object Back : Camera(
            lensPosition = back(),
            configuration = CameraConfiguration(
                    previewResolution = firstAvailable(
                            wideRatio(highestResolution()),
                            standardRatio(highestResolution())
                    ),
                    previewFpsRange = highestFps(),
                    flashMode = off(),
                    focusMode = firstAvailable(
                            continuousFocusPicture(),
                            autoFocus()
                    ),
                    frameProcessor = {
                        // Do something with the preview frame
                    }
            )
    )

    object Front : Camera(
            lensPosition = front(),
            configuration = CameraConfiguration(
                    previewResolution = firstAvailable(
                            wideRatio(highestResolution()),
                            standardRatio(highestResolution())
                    ),
                    previewFpsRange = highestFps(),
                    flashMode = off(),
                    focusMode = firstAvailable(
                            fixed(),
                            autoFocus()
                    )
            )
    )
}
