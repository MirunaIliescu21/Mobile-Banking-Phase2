package org.poo.services;

import org.poo.commands.CommandContext;
import org.poo.models.Account;
import org.poo.models.User;

public class NrOfTransactionsCashback implements CashbackStrategy {
    private static final double CASHBACK_RATE_FOOD = 0.02;
    private static final double CASHBACK_RATE_CLOTHES = 0.05;
    private static final double CASHBACK_RATE_TECH = 0.10;
    private static final int TRANSACTION_LIMIT_FOOD = 3;
    private static final int TRANSACTION_LIMIT_CLOTHES = 5;
    private static final int TRANSACTION_LIMIT_TECH = 11;
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
                                    final Account account,
                                    final double spendingAmount,
                                    final CommandContext context) {
        int transactionCount = user.getTransactionCountByCommerciant(commerciant,
                                                                     user.getTransactions());
        // Check if the user has already received cashback for the given category
        boolean hasReceivedFoodCashback = user.hasReceivedCashback("Food");
        boolean hasReceivedClothesCashback = user.hasReceivedCashback("Clothes");
        boolean hasReceivedTechCashback = user.hasReceivedCashback("Tech");

        double cashbackRate = 0;

        if (transactionCount >= TRANSACTION_LIMIT_FOOD && commerciant.getType().equals("Food")
                && !hasReceivedFoodCashback) {
            cashbackRate = CASHBACK_RATE_FOOD;
            user.setCashbackReceived("Food");
        } else if (transactionCount >= TRANSACTION_LIMIT_CLOTHES
                && commerciant.getType().equals("Clothes")
                    && !hasReceivedClothesCashback) {
            cashbackRate = CASHBACK_RATE_CLOTHES;
            user.setCashbackReceived("Clothes");
        } else if (transactionCount >= TRANSACTION_LIMIT_TECH
                && commerciant.getType().equals("Tech")
                    && !hasReceivedTechCashback) {
            cashbackRate = CASHBACK_RATE_TECH;
            user.setCashbackReceived("Tech");
        }

        double cashback = spendingAmount * cashbackRate;
        System.out.println("Cashback calculated: " + cashback);
        return cashback;
    }
}
