package org.poo.services;

/**
 * This interface defines the common behavior of all cashback strategies:
 */
import org.poo.commands.CommandContext;
import org.poo.exceptions.CurrencyConversionException;
import org.poo.models.Transaction;
import org.poo.models.User;

public interface CashbackStrategy {
    double calculateCashback(User user, Commerciant commerciant, String accountCurrency, double totalSpending, CommandContext context) throws CurrencyConversionException;
}