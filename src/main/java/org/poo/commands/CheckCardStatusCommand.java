package org.poo.commands;

import org.poo.exceptions.CardNotFoundException;
import org.poo.exceptions.InsufficientFundsException;
import org.poo.fileio.CommandInput;
import org.poo.models.Account;
import org.poo.models.Card;
import org.poo.models.Transaction;
import org.poo.models.User;

import static org.poo.commands.CommandErrors.addError;

public class CheckCardStatusCommand implements Command {
    /**
     * Check the status of a specific card.
     * Add an error to the output if the card is not found.
     * Add a transaction to the user if the balance is less than the minimum balance.
     * @param command   Command input containing email, card number, amount, and currency.
     */
    @Override
    public void execute(final CommandInput command,
                                final CommandContext context)
            throws CardNotFoundException, InsufficientFundsException {
        // Search for the card in the users' accounts
        Card card = null;
        Account cardAccount = null;
        User cardUser = null;

        for (User user : context.getUsers()) {
            for (Account account : user.getAccounts()) {
                card = account.findCardByNumber(command.getCardNumber());
                if (card != null) {
                    cardUser = user;
                    cardAccount = account;
                    break;
                }
            }
            if (card != null) {
                break;
            }
        }

        try {
            if (cardUser == null) {
                throw new CardNotFoundException("Card not found");
            }
            double balance = cardAccount.getBalance();
            double minBalance = cardAccount.getMinimumBalance();

            // Checking the conditions for "frozen" and "warning"
            if (balance <= minBalance) {
                throw new InsufficientFundsException("You have reached the minimum "
                        + "amount of funds, the card will be frozen");
            }
        } catch (CardNotFoundException e) {
            addError(context.getOutput(), e.getMessage(),
                    command.getTimestamp(), "checkCardStatus");
        } catch (InsufficientFundsException e) {
            Transaction transaction = new Transaction.TransactionBuilder(command.getTimestamp(),
                    e.getMessage(),
                    cardAccount.getIban(),
                    "error")
                    .build();
            cardUser.addTransaction(transaction);
        }
    }
}
