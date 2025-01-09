package org.poo.commands;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.poo.fileio.CommandInput;
import org.poo.models.Account;
import org.poo.models.User;

import static org.poo.commands.CommandErrors.addError;

public class ReportCommand implements Command {
    /**
     * Generate a report with the transactions of a specific account.
     * @param command   Command input containing email, card number, amount, and currency.
     */
    @Override
    public void execute(final CommandInput command,
                       final CommandContext context) {
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

        // Create the principal node for the output
        ObjectNode reportNode = context.getObjectMapper().createObjectNode();
        reportNode.put("IBAN", account.getIban());
        reportNode.put("balance", account.getBalance());
        reportNode.put("currency", account.getCurrency());

        // Select the transactions in the specified time interval
        ArrayNode transactionsArray = context.getObjectMapper().createArrayNode();
        user.printReportTransactions(transactionsArray, context.getOutput(),
                command.getStartTimestamp(), command.getEndTimestamp(), account.getIban());
        reportNode.set("transactions", transactionsArray);

        // Add the final output to the output array
        ObjectNode commandNode = context.getObjectMapper().createObjectNode();
        commandNode.put("command", "report");
        commandNode.set("output", reportNode);
        commandNode.put("timestamp", command.getTimestamp());
        context.getOutput().add(commandNode);
    }
}
