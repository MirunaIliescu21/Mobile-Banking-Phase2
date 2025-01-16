package org.poo.services;

/**
 * Represents the Student Plan.
 * This plan provides no transaction fees for its users.
 */
public class StudentPlan implements ServicePlan {

    /**
     * Gets the type of the plan.
     * @return "student" as the type of this plan.
     */
    @Override
    public String getPlanType() {
        return "student";
    }

    /**
     * Calculates the transaction fee for the Student Plan.
     * There is no fee for this plan.
     * @param amount the transaction amount.
     * @return 0, indicating no transaction fee.
     */
    @Override
    public double calculateTransactionFee(final double amount) {
        System.out.println("Transaction fee for student: " + amount * 0.0);
        return 0; // No fee for student
    }
}
