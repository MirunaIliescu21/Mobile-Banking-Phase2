package org.poo.commands;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.poo.exceptions.UserNotFoundException;
import org.poo.fileio.CommandInput;
import org.poo.models.Account;
import org.poo.models.Card;
import org.poo.models.Transaction;
import org.poo.models.User;

import static org.poo.commands.CommandErrors.addError;
import static org.poo.commands.CommandErrors.addErrorDescription;

public class DeleteAccountCommand implements  Command {
    /**
     * Deletes an account from a user and sets the status of all the cards to "destroyed".
     * If the account has a balance different from 0, throws an exception.
     * Adds success or error messages to the output as appropriate.
     *
     * @param commandInput the command to be executed
     * @param context the command execution context
     */
    @Override
    public void execute(final CommandInput commandInput,
                              final CommandContext context) {
        String accountIban = commandInput.getAccount();
        int timestamp = commandInput.getTimestamp();

        try {
            // Search for the user that has the account with the specified IBAN
            User userWithAccount = null;
            Account accountToDelete = null;

            for (User user : context.getUsers()) {
                accountToDelete = user.findAccountByIban(accountIban);
                if (accountToDelete != null) {
                    userWithAccount = user;
                    break;
                }
            }

            // If the user does not exist, add an error to the output
            if (userWithAccount == null) {
                throw new UserNotFoundException("User not found");
            }

            // Check if the account balance is non-zero
            if (accountToDelete.getBalance() != 0) {
                Transaction transaction = new Transaction.TransactionBuilder(timestamp,
                        "Account couldn't be deleted - there are funds remaining",
                        accountToDelete.getIban(), "delete")
                        .build();
                userWithAccount.addTransaction(transaction);

                throw new IllegalStateException("Account couldn't be deleted "
                        + "- see org.poo.transactions for details");
            }

            // Set the status of all the cards of the account to "destroyed"
            for (Card card : accountToDelete.getCards()) {
                card.setStatus("destroyed");
            }

            // Remove the account from the user
            userWithAccount.getAccounts().remove(accountToDelete);

            // Add a success message to the output
            ObjectNode successNode = context.getOutput().addObject();
            successNode.put("command", "deleteAccount");
            ObjectNode descriptionNode = successNode.putObject("output");
            descriptionNode.put("success", "Account deleted");
            descriptionNode.put("timestamp", timestamp);
            successNode.put("timestamp", timestamp);

        } catch (UserNotFoundException e) {
            // Handle "User not found" exception
            addError(context.getOutput(), e.getMessage(), timestamp, "deleteAccount");
        } catch (IllegalStateException e) {
            // Handle "Account couldn't be deleted" exception
            addErrorDescription(context.getOutput(), e.getMessage(), timestamp, "deleteAccount");
        }
    }
}
