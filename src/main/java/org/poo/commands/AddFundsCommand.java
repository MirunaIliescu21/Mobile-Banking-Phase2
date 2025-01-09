package org.poo.commands;

import org.poo.fileio.CommandInput;
import org.poo.models.Account;
import org.poo.models.Transaction;
import org.poo.models.User;

import static org.poo.commands.CommandErrors.addError;
import static org.poo.models.User.findUserByEmail;

public class AddFundsCommand implements Command {
    @Override
    /**
     * Add funds to a specific account.
     * @param command the command to be executed
     */
    public void execute(final CommandInput command, final CommandContext context) {
        System.out.println("addFunds " + command.getTimestamp());
        User emailUser = findUserByEmail(context.getUsers(), command.getEmail());
        if (emailUser == null) {
            addError(context.getOutput(), "User not found", command.getTimestamp(), "addFunds");
            return;
        }
        Account userAccount = emailUser.findAccountByIban(command.getAccount());

        // Add funds to the user's account
        if (userAccount != null) {
            userAccount.addFunds(command.getAmount());
            System.out.println("S-au adaugat: " + command.getAmount());
            System.out.println("account balance " + userAccount.getIban()
                    + " adaugarea de fonduri: " + userAccount.getBalance()
                    + " " + userAccount.getCurrency());
            Transaction transaction = new Transaction.TransactionBuilder(command.getTimestamp(),
                    "Funds added", userAccount.getIban(), "deposit")
                    .amount(command.getAmount())
                    .build();
            emailUser.addTransaction(transaction);
            return;
        }

        System.out.println("emailUser: " + emailUser.getEmail() + " " + emailUser.getRole());
        for (User user : context.getUsers()) {
            Account account = user.findAccountByIban(command.getAccount());
            if (account != null && !emailUser.getRole().equals("employee")) {
                account.addFunds(command.getAmount());
                System.out.println("S-au adaugat: " + command.getAmount());
                System.out.println("account balance " + account.getIban()
                        + " adaugarea de fonduri: " + account.getBalance() + " "
                        + account.getCurrency());
                Transaction transaction = new Transaction.TransactionBuilder(command.getTimestamp(),
                        "Funds added", account.getIban(), "deposit")
                        .amount(command.getAmount())
                        .build();
                emailUser.addTransaction(transaction);
                break;
            }
        }
    }

}
