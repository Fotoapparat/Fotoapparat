package io.fotoapparat.parameter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import io.fotoapparat.hardware.CameraDevice;

/**
 * Parameters of {@link CameraDevice}.
 */
public class Parameters {

    private final Map<Type, Object> values = new HashMap<>();

    /**
     * Puts value of given type, rewriting existing one (if any). Note that given value must be of
     * the type specified by {@link Type}.
     *
     * @param type  type of the parameter to store.
     * @param value value of the parameter.
     */
    public void putValue(Type type, Object value) {
        ensureType(type, value);

        values.put(type, value);
    }

    private void ensureType(Type type, Object value) {
        if (value == null) {
            return;
        }

        if (!type.clazz.isInstance(value)) {
            throw new IllegalArgumentException("Provided value must be of type " + type.clazz + ". Was " + value);
        }
    }

    /**
     * Reads stored parameter.
     *
     * @param type type of the stored parameter.
     * @return stored parameter or {@code null} if there is none.
     */
    @SuppressWarnings("unchecked")
    public <T> T getValue(Type type) {
        return (T) values.get(type);
    }

    /**
     * @return set of all stored types. That is, all types for which {@link #getValue(Type)} will
     * return non-null value.
     */
    public Set<Type> storedTypes() {
        HashSet<Type> result = new HashSet<>();

        for (Map.Entry<Type, Object> entry : values.entrySet()) {
            if (entry.getValue() != null) {
                result.add(entry.getKey());
            }
        }

        return result;
    }

    /**
     * Type of values which can be stored in {@link Parameters}.
     */
    public enum Type {

        /**
         * Size of the photo. Expected type: {@link Size}.
         */
        PICTURE_SIZE(Size.class),

        /**
         * Size of the preview stream frames. Expected type: {@link Size}.
         */
        PREVIEW_SIZE(Size.class),

        /**
         * Focus mode of the camera. Expected type: {@link FocusMode}.
         */
        FOCUS_MODE(FocusMode.class),

        /**
         * Flash firing mode of the camera. Expected type: {@link Flash}.
         */
        FLASH(Flash.class),

        /**
         * Sensor sensitivity (ISO). Expected type: (Integer).
         */
        SENSOR_SENSITIVITY(Integer.class);

        private final Class<?> clazz;

        Type(Class<?> clazz) {
            this.clazz = clazz;
        }
    }

}
