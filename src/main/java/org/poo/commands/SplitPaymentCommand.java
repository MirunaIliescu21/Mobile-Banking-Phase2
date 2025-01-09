package org.poo.commands;

import org.poo.fileio.CommandInput;
import org.poo.models.Account;
import org.poo.models.PaymentProcessor;
import org.poo.models.SplitPayment;
import org.poo.models.User;

import java.util.HashMap;
import java.util.Map;

import static org.poo.commands.CommandErrors.addError;

public class SplitPaymentCommand implements Command {
    /**
     * Split a payment between multiple users.
     * Add an error to the output if the account is not found
     * or if the currency conversion is not supported.
     * @param command   Command input containing email, card number, amount, and currency.
     */
    @Override
    public void execute(final CommandInput command, final CommandContext context) {
        PaymentProcessor paymentProcessor = PaymentProcessor.getInstance();
        Map<String, String> emailToAccount = new HashMap<>();
        Map<String, Double> accountBalances = new HashMap<>();

        for (String iban : command.getAccounts()) {
            Account account = User.findAccountByIBAN(context.getUsers(), iban);
            if (account == null) {
                addError(context.getOutput(), "Account not found: " + iban,
                        command.getTimestamp(), "splitPayment");
                return;
            }
            emailToAccount.put(account.getOwner(), account.getIban());
            accountBalances.put(account.getIban(), account.getBalance());
        }

        SplitPayment splitPayment = new SplitPayment(command.getSplitPaymentType(),
                command.getAccounts(), command.getAmountForUsers(),
                command.getCurrency(), command.getAmount(),
                command.getTimestamp(), emailToAccount, accountBalances);
        paymentProcessor.addSplitPayment(splitPayment);
        paymentProcessor.printActiveCommands();
    }
}
