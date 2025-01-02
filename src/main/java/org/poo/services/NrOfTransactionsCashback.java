package org.poo.services;

import org.poo.models.Transaction;
import org.poo.models.User;

/**
 * This strategy offers cashback based on the number of transactions.
 */
public class NrOfTransactionsCashback implements CashbackStrategy {
    @Override
    public double calculateCashback(User user, Commerciant commerciant, double spendingAmount) {
        int transactionCount = user.getTransactionCountByCommerciant(commerciant, user.getTransactions());
        System.out.println("Transaction count for " + commerciant.getName() + ": " + transactionCount);

        // Verificăm dacă utilizatorul deja a primit cashback pentru pragurile relevante
        boolean hasReceivedFoodCashback = user.hasReceivedCashback("Food");
        boolean hasReceivedClothesCashback = user.hasReceivedCashback("Clothes");
        boolean hasReceivedTechCashback = user.hasReceivedCashback("Tech");

        double cashbackRate = 0;

        if (transactionCount >= 2 && commerciant.getType().equals("Food") && !hasReceivedFoodCashback) {
            cashbackRate = 0.02;
            user.setCashbackReceived("Food");
        } else if (transactionCount >= 5 && commerciant.getType().equals("Clothes") && !hasReceivedClothesCashback) {
            cashbackRate = 0.05;
            user.setCashbackReceived("Clothes");
        } else if (transactionCount >= 10 && commerciant.getType().equals("Tech") && !hasReceivedTechCashback) {
            cashbackRate = 0.10;
            user.setCashbackReceived("Tech");
        }

        double cashback = spendingAmount * cashbackRate;
        System.out.println("Cashback calculated: " + cashback);
        return cashback;
    }
//    @Override
//    public double calculateCashback(User user, Commerciant commerciant, double spedingAmount) {
//        String category = commerciant.getType();
//        if (user.hasRedeemedDiscount(category)) {
//            System.out.println("Discount already redeemed for category: " + category);
//            return 0;
//        }
//
//        System.out.println("calculateCashback for NrOfTransactionsCashback");
//        int transactionCount = user.getTransactionCountByCommerciant(commerciant, user.getTransactions());
//        double cashbackRate = 0;
//
//        if (transactionCount >= 2 && commerciant.getType().equals("Food")) {
//            cashbackRate = 0.02;
//        }
//        if (transactionCount >= 5 && commerciant.getType().equals("Clothes")) {
//            cashbackRate = 0.05;
//        }
//        if (transactionCount >= 10 && commerciant.getType().equals("Tech")) {
//            cashbackRate = 0.10;
//        }
//        System.out.println("Cashback for " + transactionCount + " transactions: " + spedingAmount * cashbackRate);
//
//        if (cashbackRate > 0) {
//            user.redeemDiscount(category);
//        }
//        return spedingAmount * cashbackRate;
//    }
}
