package io.fotoapparat.routine.photo

import io.fotoapparat.exception.camera.CameraException
import io.fotoapparat.hardware.CameraDevice
import io.fotoapparat.hardware.Device
import io.fotoapparat.result.Photo
import kotlinx.coroutines.runBlocking

/**
 * Takes a photo.
 */

internal fun Device.takePhoto(): Photo = runBlocking {
    val cameraDevice = awaitSelectedCamera()
    cameraDevice.takePhoto()
}

internal fun Device.takePhotoRestartPreview(): Photo = runBlocking {
    val cameraDevice = awaitSelectedCamera()

    cameraDevice.takePhoto().also {
        cameraDevice.startPreviewSafely()
    }
}

private fun CameraDevice.startPreviewSafely() {
    try {
        startPreview()
    } catch (ignore: CameraException) {
    }
}

internal fun Device.startPreview(): Boolean = runBlocking {
    val cameraDevice = awaitSelectedCamera()
    cameraDevice.startPreview()
    true
}

internal fun Device.stopPreview(): Boolean = runBlocking {
    val cameraDevice = awaitSelectedCamera()
    cameraDevice.stopPreview()
    true
}

