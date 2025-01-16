package org.poo.services;

/**
 * Represents the Standard Plan.
 * This plan charges a transaction fee of 0.2% of the transaction amount.
 */
public class StandardPlan implements ServicePlan {
    private static final double TRANSACTION_FEE = 0.002;
    /**
     * Gets the type of the plan.
     * @return "standard" as the type of this plan.
     */
    @Override
    public String getPlanType() {
        return "standard";
    }

    /**
     * Calculates the transaction fee for the Standard Plan.
     * The fee is 0.2% of the transaction amount.
     * @param amount the transaction amount.
     * @return the calculated transaction fee (0.2% of the amount).
     */
    @Override
    public double calculateTransactionFee(final double amount) {
        System.out.println("Transaction fee for standard: " + amount * TRANSACTION_FEE);
        return amount * TRANSACTION_FEE; // 0.2% fee
    }
}
