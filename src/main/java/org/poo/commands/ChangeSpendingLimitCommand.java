package org.poo.commands;

import org.poo.exceptions.CurrencyConversionException;
import org.poo.fileio.CommandInput;
import org.poo.models.Account;
import org.poo.models.User;

import static org.poo.commands.CommandErrors.addError;

public class ChangeSpendingLimitCommand implements Command {
    /**
     * Change the spending limit of an account. The limit is set in RON.
     * @param command the command to be executed
     * @param context the context in which the command is executed
     */
    @Override
    public void execute(final CommandInput command, final CommandContext context) {
        Account account = User.findAccountByIBAN(context.getUsers(), command.getAccount());
        if (account == null) {
            addError(context.getOutput(), "Account not found",
                     command.getTimestamp(), "changeSpendingLimit");
            return;
        }

        if (!account.isAuthorized(command.getEmail(), "changeLimits")) {
            addError(context.getOutput(), "You must be owner in order to change spending limit.",
                    command.getTimestamp(), "changeSpendingLimit");
            return;
        }

        double amountInRON = 0;
        try {
            amountInRON = context.getCurrencyConverter().convertCurrency(command.getAmount(),
                    account.getCurrency(), "RON");
        } catch (CurrencyConversionException e) {
            addError(context.getOutput(), e.getMessage(),
                     command.getTimestamp(), "changeDepositLimit");
            return;
        }
        // Set the deposit limit in RON
        account.setSpendingLimit(amountInRON);
        System.out.println("Spending limit updated to " + command.getAmount()
                + " " + account.getCurrency()
                + " = " +  account.getSpendingLimit() + " RON at timestamp "
                + command.getTimestamp());
    }
}
