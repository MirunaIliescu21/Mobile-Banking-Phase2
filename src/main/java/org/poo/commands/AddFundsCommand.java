package org.poo.commands;

import org.poo.exceptions.CurrencyConversionException;
import org.poo.fileio.CommandInput;
import org.poo.models.Account;
import org.poo.models.Transaction;
import org.poo.models.User;

import static org.poo.commands.CommandErrors.addError;
import static org.poo.models.User.findUserByEmail;

public class AddFundsCommand implements Command {
    /**
     * Add funds to a specific account.
     * @param command the command to be executed
     */
    @Override
    public void execute(final CommandInput command, final CommandContext context)
                            throws CurrencyConversionException {
        System.out.println("addFunds " + command.getTimestamp());
        User emailUser = findUserByEmail(context.getUsers(), command.getEmail());
        if (emailUser == null) {
            addError(context.getOutput(), "User not found",
                     command.getTimestamp(), "addFunds");
            return;
        }
        Account userAccount = emailUser.findAccountByIban(command.getAccount());

        // Check if the user is a user and the account is his
        if (emailUser.getRole().equals("user") && userAccount == null) {
            System.out.println("Account not found for this user.");
            return;
        }

        // Add funds to the user's account
        if (userAccount != null) {
            AddFundsCommand.addFunds(userAccount, emailUser, command);
            return;
        }

        System.out.println("emailUser: " + emailUser.getEmail() + " " + emailUser.getRole());

        for (User user : context.getUsers()) {
            Account account = user.findAccountByIban(command.getAccount());
            if (account == null) {
                continue;
            }
            System.out.println("account: " + account.getIban() + " "
                                + account.getCurrency() + " type: " + account.getType());

            // Add funds to the owner's account
            if (!emailUser.getRole().equals("employee")) {
                AddFundsCommand.addFunds(account, emailUser, command);
                break;
            }

            // If the user is an employee, check if the amount is less than the deposit limit
            double amountInRON = context.getCurrencyConverter().convertCurrency(command.getAmount(),
                                                               account.getCurrency(), "RON");
            if (amountInRON <= account.getDepositLimit()) {
                System.out.println("Suma " + amountInRON + " este mai mica sau egala"
                        + " cu limita de depunere " + account.getDepositLimit());
                AddFundsCommand.addFunds(account, emailUser, command);
                break;
            }
        }
    }

    /**
     * Add funds to the account.
     * @param account the account to which the funds will be added
     * @param user the user that owns the account
     * @param command the command to be executed
     */
    private static void addFunds(final Account account, final User user,
                                 final CommandInput command) {
        account.addFunds(command.getAmount());
        System.out.println("S-au adaugat: " + command.getAmount());
        System.out.println("account balance " + account.getIban()
                + " adaugarea de fonduri: " + account.getBalance() + " "
                + account.getCurrency());
        Transaction transaction = new Transaction.TransactionBuilder(command.getTimestamp(),
                "Funds added", account.getIban(), "deposit")
                .amount(command.getAmount())
                .build();
        user.addTransaction(transaction);
    }
}
