package org.poo.utils;

/**
 * Enum to store cashback rates based on user plan types and thresholds.
 */
public enum CashbackRates {
    GOLD_HIGH(0.007),
    SILVER_HIGH(0.005),
    STANDARD_STUDENT_HIGH(0.0025),
    GOLD_MEDIUM(0.0055),
    SILVER_MEDIUM(0.004),
    STANDARD_STUDENT_MEDIUM(0.002),
    GOLD_LOW(0.005),
    SILVER_LOW(0.003),
    STANDARD_STUDENT_LOW(0.001),
    DEFAULT(0.0);

    private final double rate;

    CashbackRates(final double rate) {
        this.rate = rate;
    }

    /**
     * Gets the cashback rate.
     * @return the cashback rate.
     */
    public double getRate() {
        return rate;
    }
}
