package org.poo.services;

/**
 * Represents a service plan interface that defines the common behavior of all service plans.
 */
public interface ServicePlan {

    /**
     * Gets the type of the plan as a string.
     * @return the type of the plan
     */
    String getPlanType();

    /**
     * Calculates the transaction fee based on the amount provided.
     * @param amount the transaction amount.
     * @return the calculated transaction fee for the given amount.
     */
    double calculateTransactionFee(double amount);
}

