package org.poo.commands;

import org.poo.exceptions.AccountNotFoundException;
import org.poo.exceptions.UserNotFoundException;
import org.poo.fileio.CommandInput;
import org.poo.models.Account;
import org.poo.models.Transaction;
import org.poo.models.User;
import static org.poo.commands.CommandErrors.addError;

public class AddInterestCommand implements Command {
    /**
     * Collecting interest on savings accounts.
     * The interest rate is applied to the current balance.
     * If the account is not a savings account, an error is added to the output.
     * @param command the command to be executed
     * @param context the context of the command
     */
    @Override
    public void execute(final CommandInput command,
                            final CommandContext context) {
        try {
            Account account = User.findAccountByIBAN(context.getUsers(), command.getAccount());
            if (account == null) {
                throw new AccountNotFoundException("Account not found");
            }
            double rate;
            if (account.getType().equals("savings")) {
                rate = account.getInterestRate() * account.getBalance();
                System.out.println("rate: " + rate);
                account.addFunds(rate);
                System.out.println("Interest added to account " + account.getIban()
                        + " " + account.getBalance() + " " + account.getCurrency());
            } else {
                throw new IllegalArgumentException("This is not a savings account");
            }

            User user = User.findUserByAccount(context.getUsers(), account);
            if (user == null) {
                throw new UserNotFoundException("User not found");
            }

            Transaction transaction = new Transaction.TransactionBuilder(command.getTimestamp(),
                    "Interest rate income", account.getIban(), "create")
                    .amount(rate)
                    .amountCurrency(account.getCurrency())
                    .build();

            System.out.println("amount in transaction: " + transaction.getAmount());

            user.addTransaction(transaction);

        } catch (AccountNotFoundException | IllegalArgumentException | UserNotFoundException e) {
            addError(context.getOutput(), e.getMessage(),
                    command.getTimestamp(), command.getCommand());
        }
    }
}
