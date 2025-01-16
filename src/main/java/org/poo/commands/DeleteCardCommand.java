package org.poo.commands;

import org.poo.fileio.CommandInput;
import org.poo.models.Account;
import org.poo.models.Card;
import org.poo.models.Transaction;
import org.poo.models.User;

import static org.poo.commands.CommandErrors.addError;
import static org.poo.models.User.findUserByEmail;

public class DeleteCardCommand implements Command {
    /**
     * Delete a specific card and set its status to "destroyed".
     * Add a new transaction to the user.
     * @param command the command to be executed
     */
    @Override
    public void execute(final CommandInput command,
                           final CommandContext context) {
        String email = command.getEmail();
        String cardNumber = command.getCardNumber();
        int timestamp = command.getTimestamp();

        // Find the user with the specified email
        User user = findUserByEmail(context.getUsers(), email);
        if (user == null) {
            addError(context.getOutput(), "User not found", timestamp, "deleteCard");
            return;
        }

        for (Account account : user.getAccounts()) {
            // Search for the card in each account of the user
            Card cardToDelete = account.findCardByNumber(cardNumber);

            if (cardToDelete != null) {
                // Set the status of the card to "destroyed"
                cardToDelete.setStatus("destroyed");
                account.getCards().remove(cardToDelete);
                Transaction transaction = new Transaction.TransactionBuilder(timestamp,
                        "The card has been destroyed", account.getIban(), "delete")
                        .card(cardNumber)
                        .cardHolder(user.getEmail())
                        .build();
                user.addTransaction(transaction);
                return;
            }
        }
    }
}
