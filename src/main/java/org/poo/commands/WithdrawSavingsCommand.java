package org.poo.commands;

import org.poo.exceptions.CurrencyConversionException;
import org.poo.fileio.CommandInput;
import org.poo.models.Account;
import org.poo.models.Transaction;
import org.poo.models.User;

import static org.poo.commands.CommandErrors.addError;

public class WithdrawSavingsCommand implements Command {
    /**
     * Withdraws money from a savings account and adds it to the user's classic account.
     * @param command the command to be executed
     * @param context the context in which the command is executed
     */
    @Override
    public void execute(final CommandInput command, final CommandContext context) {
        String accountIBAN = command.getAccount();
        double amount = command.getAmount();
        String currency = command.getCurrency();
        System.out.println("withdrawSavings " + command.getTimestamp());
        Account account = User.findAccountByIBAN(context.getUsers(), command.getAccount());
        if (account == null) {
            addError(context.getOutput(), "Account not found",
                    command.getTimestamp(), command.getCommand());
            return;
        }

        User user = User.findUserByAccount(context.getUsers(), account);
        if (user == null) {
            addError(context.getOutput(), "User not found",
                    command.getTimestamp(), command.getCommand());
            return;
        }

        if (!account.getType().equals("savings")) {
            Transaction transaction = new Transaction.TransactionBuilder(command.getTimestamp(),
                    "Account is not of type savings.", accountIBAN, "error")
                    .build();
            user.addTransaction(transaction);
            return;
        }

        // Check if the user is of minimum age
        if (!user.isOfMinimumAge(21)) {
            Transaction transaction = new Transaction.TransactionBuilder(command.getTimestamp(),
                    "You don't have the minimum age required.", accountIBAN, "error")
                    .build();
            user.addTransaction(transaction);
            return;
        }

        // Search if the user has a classic account with the same currency
        Account classicAccount = user.getAccounts().stream()
                .filter(acc -> acc.getType().equals("classic") && acc.getCurrency().equals(currency))
                .findFirst().orElse(null);

        if (classicAccount == null) {
            Transaction transaction = new Transaction.TransactionBuilder(command.getTimestamp(),
                    "You do not have a classic account.", accountIBAN, "error")
                    .build();
            user.addTransaction(transaction);
            return;
        }

        // Check if the account has enough funds
        if (account.getBalance() < amount) {
            Transaction transaction = new Transaction.TransactionBuilder(command.getTimestamp(),
                    "Insufficient funds", accountIBAN, "error")
                    .build();
            user.addTransaction(transaction);
            return;
        }

        double convertedAmount;
        try {
            convertedAmount = context.getCurrencyConverter().convertCurrency(amount,
                    command.getCurrency(), classicAccount.getCurrency());
        } catch (CurrencyConversionException e) {
            addError(context.getOutput(), e.getMessage(),
                    command.getTimestamp(), "withdrawSavings");
            return;
        }

        account.setBalance(account.getBalance() - amount);
        classicAccount.setBalance(classicAccount.getBalance() + convertedAmount);


        Transaction transaction = new Transaction.TransactionBuilder(command.getTimestamp(),
                "Savings withdrawal", accountIBAN, "spending")
                .error(null)
                .amount(amount)
                .classicAccountIBAN(classicAccount.getIban())
                .savingsAccountIBAN(accountIBAN)
                .build();
        user.addTransaction(transaction);
        user.addTransaction(transaction);
    }
}
