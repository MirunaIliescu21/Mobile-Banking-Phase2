package org.poo.services;

import org.poo.models.Account;

public class GoldPlan implements ServicePlan {
    @Override
    public String getPlanType() {
        return "gold";
    }

    @Override
    public double calculateTransactionFee(double amount) {
        return 0; // No fee for gold
    }

    @Override
    public boolean qualifiesForUpgrade(Account account) {
        return false; // Gold is the highest tier
    }
}
