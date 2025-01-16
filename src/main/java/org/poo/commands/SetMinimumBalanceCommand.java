package org.poo.commands;

import org.poo.exceptions.AccountNotFoundException;
import org.poo.fileio.CommandInput;
import org.poo.models.Account;
import org.poo.models.User;

import static org.poo.commands.CommandErrors.addError;

public class SetMinimumBalanceCommand implements Command {
    /**
     * Set the minimum balance for a specific account.
     * @param command   Command input containing email, card number, amount, and currency.
     */
    @Override
    public void execute(final CommandInput command,
                              final CommandContext context)
            throws AccountNotFoundException {
        try {
            // Search for the user's account by Iban
            Account account = User.findAccountByIBAN(context.getUsers(), command.getAccount());
            if (account == null) {
                throw new AccountNotFoundException("Account not found");
            }
            // Set the minimum balance
            account.setMinimumBalance(command.getAmount());
        } catch (AccountNotFoundException e) {
            addError(context.getOutput(), e.getMessage(),
                    command.getTimestamp(), "setMinimumBalance");
        }
    }
}
