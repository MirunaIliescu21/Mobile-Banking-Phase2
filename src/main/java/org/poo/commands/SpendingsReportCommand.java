package org.poo.commands;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.poo.fileio.CommandInput;
import org.poo.models.Account;
import org.poo.models.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.poo.commands.CommandErrors.addError;
import static org.poo.commands.CommandErrors.addErrorType;

public class SpendingsReportCommand implements Command {
    /**
     * Generate a report with the spendings of a saving account.
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

        if (account.getType().equals("savings")) {
            addErrorType(context.getOutput(),
                    "This kind of report is not supported for a saving account",
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

        // Filter the transactions and add them to the transactions node
        ArrayNode transactionsArray = context.getObjectMapper().createArrayNode();
        Map<String, Double> spendingsByCommerciant = new HashMap<>();

        user.filterTransactionsByTypeAndInterval(transactionsArray,
                spendingsByCommerciant, command.getStartTimestamp(),
                command.getEndTimestamp(), context.getObjectMapper(), account.getIban());

        // Add the spendings by commerciant to the report
        reportNode.set("transactions", transactionsArray);

        // Create the list of commerciants with total spendings
        ArrayNode commerciantsArray = context.getObjectMapper().createArrayNode();

        // Sort the commerciants by name
        List<Map.Entry<String, Double>> sortedCommerciants;
        sortedCommerciants = new ArrayList<>(spendingsByCommerciant.entrySet());
        sortedCommerciants.sort(Map.Entry.comparingByKey());

        // Add the sort commerciants in the Json node
        for (Map.Entry<String, Double> entry : sortedCommerciants) {
            ObjectNode commerciantNode = context.getObjectMapper().createObjectNode();
            commerciantNode.put("commerciant", entry.getKey());
            commerciantNode.put("total", entry.getValue());
            commerciantsArray.add(commerciantNode);
        }

        // Add the commerciants to the report
        reportNode.set("commerciants", commerciantsArray);

        // Add the report to the output
        ObjectNode commandNode = context.getObjectMapper().createObjectNode();
        commandNode.put("command", "spendingsReport");
        commandNode.set("output", reportNode);
        commandNode.put("timestamp", command.getTimestamp());
        context.getOutput().add(commandNode);
    }
}
