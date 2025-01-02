package org.poo.services;


import org.poo.models.Account;

public class SilverPlan implements ServicePlan {
    @Override
    public String getPlanType() {
        return "silver";
    }

    @Override
    public double calculateTransactionFee(double amount) {
        return amount < 500 ? 0 : amount * 0.001; // No fee under 500 RON, 0.1% otherwise
    }

    @Override
    public boolean qualifiesForUpgrade(Account account) {
        // Check if 5 payments of at least 300 RON were made
//            return account.getRecentTransactions().stream()
//                    .filter(t -> t.getAmount() >= 300)
//                    .count() >= 5;
        return false;
    }
}
