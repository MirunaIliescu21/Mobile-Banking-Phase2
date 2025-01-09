package org.poo.commands;

import org.poo.exceptions.CurrencyConversionException;
import org.poo.fileio.CommandInput;
import org.poo.models.Account;
import org.poo.models.User;

import static org.poo.commands.CommandErrors.addError;

public class ChangeDepositLimitCommand implements Command {
    /**
     * Change the deposit limit of an account. The limit is set in RON.
     * @param command the command to be executed
     * @param context the context in which the command is executed
     */
    @Override
    public void execute(final CommandInput command, final CommandContext context) {
        Account account = User.findAccountByIBAN(context.getUsers(), command.getAccount());
        if (account == null) {
            addError(context.getOutput(), "Account not found",
                    command.getTimestamp(), "changeDepositLimit");
            return;
        }

        if (!account.isAuthorized(command.getEmail(), "changeLimits")) {
            addError(context.getOutput(), "You are not authorized to make this transaction.",
                    command.getTimestamp(), "changeDepositLimit");
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
        account.setDepositLimit(amountInRON);
        System.out.println("Deposit limit updated to " + command.getAmount()
                + " " + account.getCurrency() + " = "
                + amountInRON + " RON at timestamp " + command.getTimestamp());
    }
}
