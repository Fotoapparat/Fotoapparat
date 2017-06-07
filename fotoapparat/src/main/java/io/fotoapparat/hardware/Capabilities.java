package io.fotoapparat.hardware;

import android.support.annotation.NonNull;

import java.util.Collections;
import java.util.Set;

import io.fotoapparat.parameter.Flash;
import io.fotoapparat.parameter.FocusMode;
import io.fotoapparat.parameter.Size;
import io.fotoapparat.parameter.range.Range;

/**
 * Capabilities of camera hardware.
 */
public class Capabilities {

    @NonNull
    private final Set<Size> photoSizes;
    @NonNull
    private final Set<Size> previewSizes;
    @NonNull
    private final Set<FocusMode> focusModes;
    @NonNull
    private final Set<Flash> flashModes;
    @NonNull
    private final Range sensorSensitivityCapability;

    public Capabilities(@NonNull Set<Size> photoSizes,
                        @NonNull Set<Size> previewSizes,
                        @NonNull Set<FocusMode> focusModes,
                        @NonNull Set<Flash> flashModes,
                        @NonNull Range sensorSensitivityCapability) {
        this.photoSizes = photoSizes;
        this.previewSizes = previewSizes;
        this.focusModes = focusModes;
        this.flashModes = flashModes;
        this.sensorSensitivityCapability = sensorSensitivityCapability;
    }

    /**
     * @return Empty {@link Capabilities}.
     */
    public static Capabilities empty() {
        return new Capabilities(
                Collections.<Size>emptySet(),
                Collections.<Size>emptySet(),
                Collections.<FocusMode>emptySet(),
                Collections.<Flash>emptySet(),
                Range.<Integer>emptyRange()
        );
    }

    /**
     * @return list of supported picture sizes.
     */
    public Set<Size> supportedPictureSizes() {
        return photoSizes;
    }

    /**
     * @return list of supported preview sizes;
     */
    public Set<Size> supportedPreviewSizes() {
        return previewSizes;
    }

    /**
     * @return list of supported focus modes.
     */
    public Set<FocusMode> supportedFocusModes() {
        return focusModes;
    }

    /**
     * @return list of supported flash firing modes.
     */
    public Set<Flash> supportedFlashModes() {
        return flashModes;
    }

    /**
     * @return the sensor sensitivity capability
     */
    public Range sensorSensitivityCapability() {
        return sensorSensitivityCapability;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Capabilities that = (Capabilities) o;

        return photoSizes.equals(that.photoSizes)
                && previewSizes.equals(that.previewSizes)
                && focusModes.equals(that.focusModes)
                && flashModes.equals(that.flashModes);

    }

    @Override
    public int hashCode() {
        int result = photoSizes.hashCode();
        result = 31 * result + previewSizes.hashCode();
        result = 31 * result + focusModes.hashCode();
        result = 31 * result + flashModes.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "Capabilities{" +
                "photoSizes=" + photoSizes +
                ", previewSizes=" + previewSizes +
                ", focusModes=" + focusModes +
                ", flashModes=" + flashModes +
                ", sensorSensitivity=" + sensorSensitivityCapability +
                '}';
    }

}
