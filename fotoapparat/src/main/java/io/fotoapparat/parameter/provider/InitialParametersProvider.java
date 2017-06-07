package io.fotoapparat.parameter.provider;

import java.util.Collection;

import io.fotoapparat.hardware.CameraDevice;
import io.fotoapparat.hardware.Capabilities;
import io.fotoapparat.hardware.operators.CapabilitiesOperator;
import io.fotoapparat.parameter.Flash;
import io.fotoapparat.parameter.FocusMode;
import io.fotoapparat.parameter.Parameters;
import io.fotoapparat.parameter.Size;
import io.fotoapparat.parameter.selector.SelectorFunction;
import io.fotoapparat.parameter.range.Range;
import io.fotoapparat.parameter.selector.Selectors;

import static io.fotoapparat.parameter.selector.AspectRatioSelectors.aspectRatio;

/**
 * Provides initial {@link Parameters} for {@link CameraDevice}.
 */
public class InitialParametersProvider {

    private final InitialParametersValidator parametersValidator;
    private final CapabilitiesOperator capabilitiesOperator;
    private final SelectorFunction<Collection<Size>, Size> photoSizeSelector;
    private final SelectorFunction<Collection<Size>, Size> previewSizeSelector;
    private final SelectorFunction<Collection<FocusMode>, FocusMode> focusModeSelector;
    private final SelectorFunction<Collection<Flash>, Flash> flashSelector;
    private final SelectorFunction<Range<Integer>, Integer> sensorSensitivitySelector;

    public InitialParametersProvider(CapabilitiesOperator capabilitiesOperator,
                                     SelectorFunction<Collection<Size>, Size> photoSizeSelector,
                                     SelectorFunction<Collection<Size>, Size> previewSizeSelector,
                                     SelectorFunction<Collection<FocusMode>, FocusMode> focusModeSelector,
                                     SelectorFunction<Collection<Flash>, Flash> flashSelector,
                                     SelectorFunction<Range<Integer>, Integer> sensorSensitivitySelector,
                                     InitialParametersValidator parametersValidator) {
        this.capabilitiesOperator = capabilitiesOperator;
        this.photoSizeSelector = photoSizeSelector;
        this.previewSizeSelector = previewSizeSelector;
        this.focusModeSelector = focusModeSelector;
        this.flashSelector = flashSelector;
        this.sensorSensitivitySelector = sensorSensitivitySelector;
        this.parametersValidator = parametersValidator;
    }

    /**
     * @return {@link Parameters} which will be used by {@link CameraDevice} on start-up.
     */
    public Parameters initialParameters() {
        Capabilities capabilities = capabilitiesOperator.getCapabilities();

        Parameters parameters = new Parameters();

        putPictureSize(capabilities, parameters);
        putPreviewSize(capabilities, parameters);
        putFocusMode(capabilities, parameters);
        putFlash(capabilities, parameters);
        putSensorSensitivity(capabilities, parameters);

        parametersValidator.validate(parameters);

        return parameters;
    }

    private void putPreviewSize(Capabilities capabilities, Parameters parameters) {
        Size photoSize = photoSize(capabilities);

        parameters.putValue(
                Parameters.Type.PREVIEW_SIZE,
                Selectors
                        .firstAvailable(
                                previewWithSameAspectRatio(photoSize),
                                previewSizeSelector
                        )
                        .select(capabilities.supportedPreviewSizes())
        );
    }

    private SelectorFunction<Collection<Size>, Size> previewWithSameAspectRatio(Size photoSize) {
        return aspectRatio(
                photoSize.getAspectRatio(),
                previewSizeSelector
        );
    }

    private void putPictureSize(Capabilities capabilities, Parameters parameters) {
        parameters.putValue(
                Parameters.Type.PICTURE_SIZE,
                photoSize(capabilities)
        );
    }

    private Size photoSize(Capabilities capabilities) {
        return photoSizeSelector.select(
                capabilities.supportedPictureSizes()
        );
    }

    private void putFocusMode(Capabilities capabilities, Parameters parameters) {
        parameters.putValue(
                Parameters.Type.FOCUS_MODE,
                focusModeSelector.select(
                        capabilities.supportedFocusModes()
                )
        );
    }

    private void putFlash(Capabilities capabilities, Parameters parameters) {
        parameters.putValue(
                Parameters.Type.FLASH,
                flashSelector.select(
                        capabilities.supportedFlashModes()
                )
        );
    }

    private void putSensorSensitivity(Capabilities capabilities, Parameters parameters) {
        parameters.putValue(
                Parameters.Type.SENSOR_SENSITIVITY,
                sensorSensitivitySelector.select(
                        capabilities.sensorSensitivityCapability()
                )
        );
    }

}
