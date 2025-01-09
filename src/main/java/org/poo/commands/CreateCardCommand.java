package org.poo.commands;

import org.poo.fileio.CommandInput;
import org.poo.models.Account;
import org.poo.models.Card;
import org.poo.models.Transaction;
import org.poo.models.User;
import org.poo.utils.Utils;

import static org.poo.models.User.findUserByEmail;

public class CreateCardCommand implements Command {
    @Override
    /**
     * Create a card for a specific account.
     * Add a new transaction to the user.
     * @param command the command to be executed
     */
    public void execute(final CommandInput command, final CommandContext context) {
        User user = findUserByEmail(context.getUsers(), command.getEmail());
        if (user != null) {
            Account account = user.findAccountByIban(command.getAccount());
            if (account != null) {
                String cardNumber = Utils.generateCardNumber();
                account.addCard(new Card(cardNumber, "active", "normal"));
                Transaction transaction = new Transaction.TransactionBuilder(command.getTimestamp(),
                        "New card created", account.getIban(), "create")
                        .card(cardNumber)
                        .cardHolder(user.getEmail())
                        .build();
                user.addTransaction(transaction);
            }
        }
    }
}
