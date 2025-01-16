package org.poo.commands;

import org.poo.fileio.CommandInput;
import org.poo.models.Account;
import org.poo.models.User;

import static org.poo.commands.CommandErrors.addError;

public class AddNewBusinessAssociateCommand implements Command {
    /**
     * Adds a new business associate to the account.
     * The associate maybe an employee or a manager.
     * @param command the command to be executed
     * @param context the context in which the command is executed
     */
    @Override
    public void execute(final CommandInput command, final CommandContext context) {
        Account account = User.findAccountByIBAN(context.getUsers(), command.getAccount());
        if (account == null) {
            addError(context.getOutput(), "Account not found",
                    command.getTimestamp(), "addNewBusinessAssociate");
            return;
        }

        User user = User.findUserByEmail(context.getUsers(), command.getEmail());
        if (user == null) {
            addError(context.getOutput(), "User not found",
                    command.getTimestamp(), "addNewBusinessAssociate");
            return;
        }
        user.setOwnerAccount(account);
        user.setRole(command.getRole());
        account.addAssociate(command.getEmail(), command.getRole());
        System.out.println("Added new associate: " + command.getTimestamp()
                            + " " + command.getEmail() + " as " + command.getRole()
                            + " to account " + account.getIban());
    }
}
