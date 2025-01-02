package org.poo.services;

import org.poo.models.Account;

public class StudentPlan implements ServicePlan {
    @Override
    public String getPlanType() {
        return "student";
    }

    @Override
    public double calculateTransactionFee(double amount) {
        return 0; // No fee for student
    }

    @Override
    public boolean qualifiesForUpgrade(Account account) {
        return false; // No automatic upgrade for student
    }
}
