package org.poo.services;

import org.poo.commands.CommandContext;
import org.poo.exceptions.CurrencyConversionException;
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
     * @param accountCurrency The currency of the user's account.
     * @param totalSpending The total spending amount of the user.
     * @param context The command context providing system utilities (e.g., currency conversion).
     * @return The calculated cashback amount.
     * @throws CurrencyConversionException If currency conversion fails.
     */
    double calculateCashback(final User user, final Commerciant commerciant,
                             final String accountCurrency, final double totalSpending,
                             final CommandContext context) throws CurrencyConversionException;
}