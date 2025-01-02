package org.poo.services;

import org.poo.models.Account;

public class StandardPlan implements ServicePlan {
    @Override
    public String getPlanType() {
        return "standard";
    }

    @Override
    public double calculateTransactionFee(double amount) {
        return amount * 0.002; // 0.2% fee
    }

    @Override
    public boolean qualifiesForUpgrade(Account account) {
        return false; // No automatic upgrade for standard
    }
}
