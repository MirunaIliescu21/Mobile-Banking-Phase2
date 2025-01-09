package org.poo.services;

import org.poo.commands.CommandContext;
import org.poo.models.User;

public class NrOfTransactionsCashback implements CashbackStrategy {
    /**
     * Implements cashback calculation based on the number of transactions with a merchant.
     *
     * Cashback rates vary depending on the merchant's business type:
     * - "Food": 2% cashback after 3 transactions.
     * - "Clothes": 5% cashback after 5 transactions.
     * - "Tech": 10% cashback after 11 transactions.
     * Cashback is only awarded once per category.
     */
    @Override
    public double calculateCashback(final User user, final Commerciant commerciant,
                                    final String accountCurrency, final double spendingAmount,
                                    final CommandContext context) {

        int transactionCount = user.getTransactionCountByCommerciant(commerciant, user.getTransactions());
        System.out.println("Transaction count for " + commerciant.getName() + ": " + transactionCount);

        // Check if the user has already received cashback for the given category
        boolean hasReceivedFoodCashback = user.hasReceivedCashback("Food");
        boolean hasReceivedClothesCashback = user.hasReceivedCashback("Clothes");
        boolean hasReceivedTechCashback = user.hasReceivedCashback("Tech");

        double cashbackRate = 0;

        if (transactionCount >= 3 && commerciant.getType().equals("Food")
                && !hasReceivedFoodCashback) {
            cashbackRate = 0.02;
            user.setCashbackReceived("Food");
        } else if (transactionCount >= 5 && commerciant.getType().equals("Clothes")
                    && !hasReceivedClothesCashback) {
            cashbackRate = 0.05;
            user.setCashbackReceived("Clothes");
        } else if (transactionCount >= 11 && commerciant.getType().equals("Tech")
                    && !hasReceivedTechCashback) {
            cashbackRate = 0.10;
            user.setCashbackReceived("Tech");
        }

        double cashback = spendingAmount * cashbackRate;
        System.out.println("Cashback calculated: " + cashback);
        return cashback;
    }
}
