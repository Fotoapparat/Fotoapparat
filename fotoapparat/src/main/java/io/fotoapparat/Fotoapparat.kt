package io.fotoapparat

import android.content.Context
import androidx.annotation.FloatRange
import io.fotoapparat.concurrent.CameraExecutor
import io.fotoapparat.concurrent.CameraExecutor.Operation
import io.fotoapparat.configuration.CameraConfiguration
import io.fotoapparat.configuration.Configuration
import io.fotoapparat.error.CameraErrorCallback
import io.fotoapparat.error.onMainThread
import io.fotoapparat.hardware.Device
import io.fotoapparat.hardware.display.Display
import io.fotoapparat.hardware.orientation.OrientationSensor
import io.fotoapparat.log.Logger
import io.fotoapparat.log.none
import io.fotoapparat.parameter.ScaleType
import io.fotoapparat.result.*
import io.fotoapparat.routine.camera.bootStart
import io.fotoapparat.routine.camera.shutDown
import io.fotoapparat.routine.camera.switchCamera
import io.fotoapparat.routine.camera.updateDeviceConfiguration
import io.fotoapparat.routine.capability.getCapabilities
import io.fotoapparat.routine.focus.focus
import io.fotoapparat.routine.parameter.getCurrentParameters
import io.fotoapparat.routine.photo.startPreview
import io.fotoapparat.routine.photo.stopPreview
import io.fotoapparat.routine.photo.takePhoto
import io.fotoapparat.routine.photo.takePhotoRestartPreview
import io.fotoapparat.routine.zoom.updateZoomLevel
import io.fotoapparat.selector.*
import io.fotoapparat.view.CameraRenderer
import io.fotoapparat.view.FocalPointSelector

/**
 * Camera. Takes pictures.
 */
class Fotoapparat
@JvmOverloads constructor(
        context: Context,
        view: CameraRenderer,
        focusView: FocalPointSelector? = null,
        lensPosition: LensPositionSelector = firstAvailable(
                back(),
                front(),
                external()
        ),
        scaleType: ScaleType = ScaleType.CenterCrop,
        cameraConfiguration: CameraConfiguration = CameraConfiguration.default(),
        cameraErrorCallback: CameraErrorCallback = {},
        private val executor: CameraExecutor = EXECUTOR,
        private val logger: Logger = none()
) {

    private val mainThreadErrorCallback = cameraErrorCallback.onMainThread()

    private val display = Display(context)

    private val device = Device(
            cameraRenderer = view,
            focusPointSelector = focusView,
            logger = logger,
            display = display,
            scaleType = scaleType,
            initialLensPositionSelector = lensPosition,
            initialConfiguration = cameraConfiguration,
            executor = executor
    )

    private val orientationSensor by lazy {
        OrientationSensor(
                context = context,
                device = device
        )
    }

    init {
        logger.recordMethod()
    }

    /**
     * Starts camera.
     *
     * @throws IllegalStateException If the camera has already started.
     */
    fun start() {
        logger.recordMethod()

        executor.execute(Operation {
            device.bootStart(
                    orientationSensor = orientationSensor,
                    mainThreadErrorCallback = mainThreadErrorCallback
            )
        })
    }

    /**
     * Stops camera.
     *
     * @throws IllegalStateException If the camera has not started.
     */
    fun stop() {
        logger.recordMethod()

        executor.cancelTasks()
        executor.execute(Operation {
            device.shutDown(
                    orientationSensor = orientationSensor
            )
        })
    }

    /**
     * Takes picture, returns immediately.
     *
     * @return [PhotoResult] which will deliver result asynchronously.
     */
    fun takePicture(): PhotoResult {
        logger.recordMethod()
        return takePicture(restartPreview = true)
    }

    /**
     * Takes picture with ability to disable automatically restart preview, returns immediately.
     *
     * @return [PhotoResult] which will deliver result asynchronously.
     */
    fun takePicture(restartPreview: Boolean): PhotoResult {
        logger.recordMethod()

        val future = executor.execute(Operation(
                cancellable = true,
                function = if(restartPreview) device::takePhotoRestartPreview else device::takePhoto
        ))

        return PhotoResult.fromFuture(future, logger)
    }

    /**
     * Start preview manually, returns immediately.
     *
     * @return [BooleanResult] which will deliver result asynchronously.
     */
    fun startPreview(): BooleanResult {
        logger.recordMethod()

        val future = executor.execute(Operation(
                cancellable = true,
                function = device::startPreview
        ))

        return PendingResult.fromFuture(future, logger)
    }

    /**
     * Stop preview manually, returns immediately.
     *
     * @return [BooleanResult] which will deliver result asynchronously.
     */
    fun stopPreview(): BooleanResult {
        logger.recordMethod()

        val future = executor.execute(Operation(
                cancellable = true,
                function = device::stopPreview
        ))

        return PendingResult.fromFuture(future, logger)
    }

    /**
     * Provides camera capabilities asynchronously, returns immediately.
     *
     * @return [CapabilitiesResult] which will deliver result asynchronously.
     */
    fun getCapabilities(): CapabilitiesResult {
        logger.recordMethod()

        val future = executor.execute(Operation(
                cancellable = true,
                function = device::getCapabilities
        ))

        return PendingResult.fromFuture(future, logger)
    }

    /**
     * Provides current camera parameters asynchronously, returns immediately.
     *
     * @return [ParametersResult] which will deliver result asynchronously.
     */
    fun getCurrentParameters(): ParametersResult {
        logger.recordMethod()

        val future = executor.execute(Operation(
                cancellable = true,
                function = device::getCurrentParameters
        ))

        return PendingResult.fromFuture(future, logger)
    }

    /**
     * Updates current configuration.
     *
     * @throws IllegalStateException If the current camera has not started.
     */
    fun updateConfiguration(newConfiguration: Configuration) = executor.execute(
            Operation(cancellable = true) {
                logger.recordMethod()

                device.updateDeviceConfiguration(newConfiguration)
            })

    /**
     * Asynchronously updates zoom level of the camera.
     * If zoom is not supported by the device - does nothing.
     *
     * @param zoomLevel Zoom level of the camera. A value between 0 and 1.
     * @throws IllegalStateException If the current camera has not started.
     */
    fun setZoom(@FloatRange(from = 0.0, to = 1.0) zoomLevel: Float) = executor.execute(
            Operation(cancellable = true) {
                logger.recordMethod()

                device.updateZoomLevel(
                        zoomLevel = zoomLevel
                )
            })

    /**
     * Performs auto focus. If it is not available or not enabled, does nothing.
     *
     * @see Fotoapparat.focus
     */
    fun autoFocus(): Fotoapparat = apply {
        logger.recordMethod()
        focus()
    }

    /**
     * Attempts to focus the camera asynchronously.
     *
     * @return the pending result of focus operation which will deliver result asynchronously.
     *
     * @see Fotoapparat.autoFocus
     */
    fun focus(): PendingResult<FocusResult> {
        logger.recordMethod()

        val future = executor.execute(Operation(
                cancellable = true,
                function = device::focus
        ))

        return PendingResult.fromFuture(future, logger)
    }

    /**
     * Switches to another camera. If previous camera has already started then it will be
     * stopped automatically and new will start.
     */
    fun switchTo(
            lensPosition: LensPositionSelector,
            cameraConfiguration: CameraConfiguration
    ) {
        logger.recordMethod()

        executor.execute(Operation(cancellable = true) {
            device.switchCamera(
                    orientationSensor = orientationSensor,
                    newLensPositionSelector = lensPosition,
                    newConfiguration = cameraConfiguration,
                    mainThreadErrorCallback = mainThreadErrorCallback
            )
        })
    }

    /**
     * @return `true` if selected lens position is available. `false` if it is not available.
     */
    fun isAvailable(
            selector: LensPositionSelector
    ): Boolean = device.canSelectCamera(selector)

    companion object {

        private val EXECUTOR = CameraExecutor()

        @JvmStatic
        fun with(context: Context): FotoapparatBuilder = FotoapparatBuilder(context)

    }
}
