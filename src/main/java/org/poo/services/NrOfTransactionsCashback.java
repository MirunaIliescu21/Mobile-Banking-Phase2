package org.poo.services;

import org.poo.models.Transaction;
import org.poo.models.User;

/**
 * This strategy offers cashback based on the number of transactions.
 */
public class NrOfTransactionsCashback implements CashbackStrategy {
    @Override
    public double calculateCashback(User user, Commerciant commerciant, double spedingAmount) {
        int transactionCount = user.getTransactionCountByCommerciant(commerciant);
        double cashbackRate = 0;

        if (transactionCount >= 2 && commerciant.getType().equals("Food")) {
            cashbackRate = 0.02;
        }
        if (transactionCount >= 5 && commerciant.getType().equals("Clothes")) {
            cashbackRate = 0.05;
        }
        if (transactionCount >= 10 && commerciant.getType().equals("Tech")) {
            cashbackRate = 0.10;
        }

        return spedingAmount * cashbackRate;
    }
}
