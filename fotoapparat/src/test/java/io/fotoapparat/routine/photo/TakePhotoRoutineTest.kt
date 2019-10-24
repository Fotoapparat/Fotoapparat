package io.fotoapparat.routine.photo

import io.fotoapparat.exception.camera.CameraException
import io.fotoapparat.hardware.CameraDevice
import io.fotoapparat.hardware.Device
import io.fotoapparat.result.Photo
import io.fotoapparat.test.willReturn
import io.fotoapparat.test.willThrow
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.inOrder
import org.mockito.junit.MockitoJUnitRunner
import kotlin.test.assertEquals

@RunWith(MockitoJUnitRunner::class)
internal class TakePhotoRoutineTest {

    @Mock
    lateinit var cameraDevice: CameraDevice
    @Mock
    lateinit var device: Device

    @Test
    fun `Take photo`() = runBlocking {
        // Given
        val photo = Photo.empty()
        device.awaitSelectedCamera() willReturn cameraDevice
        cameraDevice.takePhoto() willReturn photo

        // When
        val result = device.takePhotoRestartPreview()

        // Then
        val inOrder = inOrder(cameraDevice)
        inOrder.apply {
            verify(cameraDevice).takePhoto()
            verify(cameraDevice).startPreview()
        }

        assertEquals(
                expected = photo,
                actual = result
        )
    }

    @Test
    fun `Suppress restarting preview exception`() = runBlocking {
        // Given
        val photo = Photo.empty()
        device.awaitSelectedCamera() willReturn cameraDevice
        cameraDevice.takePhoto() willReturn photo
        cameraDevice.startPreview() willThrow CameraException("test")

        // When
        val result = device.takePhotoRestartPreview()

        // Then
        val inOrder = inOrder(cameraDevice)
        inOrder.apply {
            verify(cameraDevice).takePhoto()
            verify(cameraDevice).startPreview()
        }

        assertEquals(
                expected = photo,
                actual = result
        )
    }
}
