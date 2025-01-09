package org.poo.commands;

import org.poo.exceptions.AccountNotFoundException;
import org.poo.exceptions.UserNotFoundException;
import org.poo.fileio.CommandInput;
import org.poo.models.Account;
import org.poo.models.User;
import static org.poo.commands.CommandErrors.addError;
import static org.poo.models.User.findUserByEmail;

public class SetAliasCommand implements Command {
    /**
     * Make an alias for a specific account.
     * @param command   Command input containing email, card number, amount, and currency.
     */
    @Override
    public void execute(final CommandInput command,
                            final CommandContext context) {
        String email = command.getEmail();
        String alias = command.getAlias();

        try {
            User user = findUserByEmail(context.getUsers(), email);
            if (user == null) {
                throw new UserNotFoundException("User not found");
            }
            Account account = user.findAccountByIban(command.getAccount());
            if (account == null) {
                throw new AccountNotFoundException("Account not found");
            }
            // Check if the alias is already in use
            if (user.hasAlias(alias)) {
                throw new IllegalArgumentException("Alias already in use");
            }
            // Set the alias for the account
            account.setAlias(alias);
        } catch (UserNotFoundException | AccountNotFoundException e) {
            addError(context.getOutput(), e.getMessage(),
                    command.getTimestamp(), "makeAnAlias");
        } catch (IllegalArgumentException e) {
            return;
        }
    }
}
