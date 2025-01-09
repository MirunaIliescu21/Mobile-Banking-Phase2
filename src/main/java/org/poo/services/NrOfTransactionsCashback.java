package org.poo.services;

import org.poo.commands.CommandContext;
import org.poo.exceptions.CurrencyConversionException;
import org.poo.models.Transaction;
import org.poo.models.User;

/**
 * This strategy offers cashback based on the number of transactions.
 */
public class NrOfTransactionsCashback implements CashbackStrategy {
    @Override
    public double calculateCashback(User user, Commerciant commerciant, String accountCurrency, double spendingAmount, CommandContext context) {

        int transactionCount = user.getTransactionCountByCommerciant(commerciant, user.getTransactions());
        System.out.println("Transaction count for " + commerciant.getName() + ": " + transactionCount);

        // Verificăm dacă utilizatorul deja a primit cashback pentru pragurile relevante
        boolean hasReceivedFoodCashback = user.hasReceivedCashback("Food");
        boolean hasReceivedClothesCashback = user.hasReceivedCashback("Clothes");
        boolean hasReceivedTechCashback = user.hasReceivedCashback("Tech");

        double cashbackRate = 0;

        if (transactionCount >= 3 && commerciant.getType().equals("Food") && !hasReceivedFoodCashback) {
            cashbackRate = 0.02;
            user.setCashbackReceived("Food");
        } else if (transactionCount >= 5 && commerciant.getType().equals("Clothes") && !hasReceivedClothesCashback) {
            cashbackRate = 0.05;
            user.setCashbackReceived("Clothes");
        } else if (transactionCount >= 11 && commerciant.getType().equals("Tech") && !hasReceivedTechCashback) {
            cashbackRate = 0.10;
            user.setCashbackReceived("Tech");
        }

        double cashback = spendingAmount * cashbackRate;
        System.out.println("Cashback calculated: " + cashback);
        return cashback;
    }
}
