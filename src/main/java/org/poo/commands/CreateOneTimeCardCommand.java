package org.poo.commands;

import org.poo.exceptions.AccountNotFoundException;
import org.poo.exceptions.UserNotFoundException;
import org.poo.fileio.CommandInput;
import org.poo.models.Account;
import org.poo.models.Card;
import org.poo.models.Transaction;
import org.poo.models.User;
import org.poo.utils.Utils;

import static org.poo.commands.CommandErrors.addError;
import static org.poo.models.User.findUserByEmail;

public class CreateOneTimeCardCommand implements Command {
    @Override
    /**
     * Create a "one time pay" card for a specific account.
     * Add a new transaction to the user.
     * @param command the command to be executed
     */
    public void execute(final CommandInput command, final CommandContext context) {
        try {
            User user = findUserByEmail(context.getUsers(), command.getEmail());

            if (user == null) {
                throw new UserNotFoundException("User not found");

            }
            Account account = user.findAccountByIban(command.getAccount());
            if (account == null) {
                throw new AccountNotFoundException("Account not found");
            }

            // Generate the card number and create the card
            String cardNumber = Utils.generateCardNumber();
            Card oneTimeCard = new Card(cardNumber, "active", "one time pay");

            account.addCard(oneTimeCard);

            // Add a success transaction to the user
            Transaction transaction = new Transaction.TransactionBuilder(command.getTimestamp(),
                    "New card created", account.getIban(), "create")
                    .card(cardNumber)
                    .cardHolder(user.getEmail())
                    .build();
            user.addTransaction(transaction);
        } catch (UserNotFoundException | AccountNotFoundException e) {
            addError(context.getOutput(), e.getMessage(),
                    command.getTimestamp(), "createOneTimeCard");
        }
    }

}
