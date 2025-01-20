package org.poo.services;

/**
 * Represents the Silver Plan.
 * This plan charges no fee for transactions under 500 RON, and a fee of 0.1% for larger amounts.
 */
public class SilverPlan implements ServicePlanStrategy {
    private static final double TRANSACTION_FEE = 0.001;
    private static final double MAX_TRANSACTION_FEE = 500;
    /**
     * Gets the type of the plan.
     * @return "silver" as the type of this plan.
     */
    @Override
    public String getPlanType() {
        return "silver";
    }

    /**
     * Calculates the transaction fee for the Silver Plan.
     * No fee is charged for transactions under 500 RON. A fee of 0.1% is charged otherwise.
     * @param amount the transaction amount.
     * @return the calculated transaction fee (0.1% for amounts >= 500 RON, otherwise 0).
     */
    @Override
    public double calculateTransactionFee(final double amount) {
        // No fee under 500 RON, 0.1% otherwise
        return amount < MAX_TRANSACTION_FEE ? 0 : amount * TRANSACTION_FEE;
    }
}
