package org.poo.services;

import org.poo.commands.CommandContext;
import org.poo.exceptions.CurrencyConversionException;
import org.poo.models.Account;
import org.poo.models.User;

/**
 * Defines a contract for cashback strategies.
 * Implementing classes must calculate cashback based on user transactions
 * and the merchant's details.
 */
public interface CashbackStrategy {
    /**
     * Calculate cashback for a specific user based on spending or transaction data.
     *
     * @param user The user making the transaction.
     * @param commerciant The merchant associated with the transaction.
     * @param account The user's account for this transaction.
     * @param totalSpending The total spending amount of the user.
     * @param context The command context providing system utilities (e.g., currency conversion).
     * @return The calculated cashback amount.
     * @throws CurrencyConversionException If currency conversion fails.
     */
    double calculateCashback(User user, Commerciant commerciant,
                             Account account, double totalSpending,
                             CommandContext context) throws CurrencyConversionException;
}
