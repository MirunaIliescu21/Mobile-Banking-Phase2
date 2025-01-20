package org.poo.services;

/**
 * Represents the Gold Plan.
 * This plan provides no transaction fees for its users.
 */
public class GoldPlan implements ServicePlanStrategy {

    /**
     * Gets the type of the plan.
     * @return "gold" as the type of this plan.
     */
    @Override
    public String getPlanType() {
        return "gold";
    }

    /**
     * Calculates the transaction fee for the Gold Plan.
     * There is no fee for this plan.
     * @param amount the transaction amount.
     * @return 0, indicating no transaction fee.
     */
    @Override
    public double calculateTransactionFee(final double amount) {
        return 0; // No fee for gold
    }
}
