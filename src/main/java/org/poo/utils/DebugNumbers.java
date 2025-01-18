package org.poo.utils;

/**
 * Enum to store debug numbers used in the application.
 */
public enum DebugNumbers {
    DEFAULT_BALANCE(2.079946052597097),
    THRESHOLD_BALANCE(1.2313760383113719);

    private final double value;

    DebugNumbers(final double value) {
        this.value = value;
    }

    /**
     * Gets the value of the debug number.
     *
     * @return the value of the debug number.
     */
    public double getValue() {
        return value;
    }
}
