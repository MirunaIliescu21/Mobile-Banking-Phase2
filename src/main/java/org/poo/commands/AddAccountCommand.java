package org.poo.commands;

import org.poo.exceptions.UserNotFoundException;
import org.poo.fileio.CommandInput;
import org.poo.models.Account;
import org.poo.models.Transaction;
import org.poo.models.User;
import org.poo.utils.Utils;

import static org.poo.commands.CommandErrors.addError;
import static org.poo.models.User.findUserByEmail;

public class AddAccountCommand implements  Command {
    /**
     * Add an account to a user.
     * Add a new Transaction to the user.
     * @param command the command to be executed
     */
    @Override
    public void execute(final CommandInput command, final CommandContext context) {
        System.out.println("addAccount " + command.getTimestamp() + " " + command.getAccountType());
        User user = findUserByEmail(context.getUsers(), command.getEmail());
        try {
            if (user == null) {
                throw new UserNotFoundException("User not found");
            }

            String iban = Utils.generateIBAN();
            user.addAccount(new Account(iban, command.getCurrency(),
                    command.getAccountType(), command.getEmail()));

            if (command.getAccountType().equals("savings")) {
                user.findAccountByIban(iban).setInterestRate(command.getInterestRate());
            }

            if (command.getAccountType().equals("business")) {
                user.setRole("owner");
            }

            Transaction transaction = new Transaction.TransactionBuilder(command.getTimestamp(),
                    "New account created", iban, "create")
                    .build();
            user.addTransaction(transaction);
        } catch (UserNotFoundException e) {
            addError(context.getOutput(), e.getMessage(),
                    command.getTimestamp(), "addAccount");
        }
    }
}
