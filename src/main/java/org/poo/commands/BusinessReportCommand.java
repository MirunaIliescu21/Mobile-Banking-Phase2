package org.poo.commands;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.poo.exceptions.CurrencyConversionException;
import org.poo.fileio.CommandInput;
import org.poo.models.Account;
import org.poo.models.Transaction;
import org.poo.models.User;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.poo.commands.CommandErrors.addError;

public class BusinessReportCommand implements Command {
    /**
     * Executes the businessReport command.
     *
     * @param command the command input containing details for execution
     * @param context the context of the command, including users and services
     */
    @Override
    public void execute(final CommandInput command, final CommandContext context) {
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
        double depositLimit = account.getDepositLimit();
        double spendingLimit = account.getSpendingLimit();
        try {
            depositLimit = context.getCurrencyConverter().convertCurrency(depositLimit,
                                                                "RON", account.getCurrency());
            spendingLimit = context.getCurrencyConverter().convertCurrency(spendingLimit,
                                                                "RON", account.getCurrency());
        } catch (CurrencyConversionException e) {
            addError(context.getOutput(), e.getMessage(),
                    command.getTimestamp(), command.getCommand());
            return;
        }

        // Create report node
        ObjectNode reportNode = context.getObjectMapper().createObjectNode();
        reportNode.put("IBAN", account.getIban());
        reportNode.put("balance", (account.getBalance()));
        reportNode.put("currency", account.getCurrency());
        reportNode.put("spending limit", (spendingLimit));
        reportNode.put("deposit limit", (depositLimit));
        reportNode.put("statistics type", command.getType());

        // Generate the report based on type
        switch (command.getType()) {
            case "transaction":
                generateTransactionReport(reportNode, command, account, context);
                break;

            case "commerciant":
                generateCommerciantReport(reportNode, command, account, context);
                break;

            default:
                addError(context.getOutput(), "Invalid report type",
                        command.getTimestamp(), command.getCommand());
                return;
        }


        // Add the report to context output
        ObjectNode commandNode = context.getObjectMapper().createObjectNode();
        commandNode.put("command", "businessReport");
        commandNode.set("output", reportNode);
        commandNode.put("timestamp", command.getTimestamp());
        context.getOutput().add(commandNode);
    }

    private void generateTransactionReport(final ObjectNode reportNode, final CommandInput command,
                                           final Account account, final CommandContext context) {
        System.out.println("Business report transaction " + account.getIban());

        ArrayNode managersArray = context.getObjectMapper().createArrayNode();
        ArrayNode employeesArray = context.getObjectMapper().createArrayNode();

        double totalSpent = 0;
        double totalDeposited = 0;

        // Iterate through the account associates
        for (Map.Entry<String, String> entry : account.getAssociates().entrySet()) {
            String email = entry.getKey();
            String role = entry.getValue();

            User user = User.findUserByEmail(context.getUsers(), email);
            if (user == null) {
                continue;
            }

            List<Transaction> transactions = User.getTransactionsInRange(user,
                    command.getStartTimestamp(), command.getEndTimestamp(), account.getIban());

            double spent = 0;
            double deposited = 0;

            // Make the sum of the spent and deposited amounts
            for (Transaction transaction : transactions) {
                // System.out.println("transaction type: " + transaction.getType()
                // + " amount: " + transaction.getAmount());
                if (transaction.getType().equals("spending")) {
                    spent += transaction.getAmount();
                } else if (transaction.getType().equals("deposit")) {
                    deposited += transaction.getAmount();
                }
            }

            ObjectNode associateNode = context.getObjectMapper().createObjectNode();
            associateNode.put("username", user.getLastName() + " " + user.getFirstName());
            associateNode.put("spent", spent);
            associateNode.put("deposited", deposited);

            // Add the node to the corresponding array
            if ("manager".equals(role)) {
                managersArray.add(associateNode);
            } else if ("employee".equals(role)) {
                employeesArray.add(associateNode);
            }

            // Add the spent and deposited amounts to the total
            totalSpent += spent;
            totalDeposited += deposited;
        }

        reportNode.set("managers", managersArray);
        reportNode.set("employees", employeesArray);
        reportNode.put("total spent", totalSpent);
        reportNode.put("total deposited", totalDeposited);
    }

    private void generateCommerciantReport(final ObjectNode reportNode, final CommandInput command,
                                           final Account account, final CommandContext context) {
        System.out.println("Business report commerciant " + account.getIban());

        // HashMap for commerciant data
        Map<String, ObjectNode> commerciantData = new TreeMap<>(); // Sortat alfabetic

        // Iterate through the account associates
        for (Map.Entry<String, String> entry : account.getAssociates().entrySet()) {
            String email = entry.getKey();
            String role = entry.getValue();

            User user = User.findUserByEmail(context.getUsers(), email);
            if (user == null) {
                continue;
            }

            // Get the transactions for the user in the specified time range
            List<Transaction> transactions = User.getTransactionsInRange(user,
                    command.getStartTimestamp(), command.getEndTimestamp(), account.getIban());

            for (Transaction transaction : transactions) {
                if (!transaction.getType().equals("spending")) {
                    continue; // Ignore non-spending transactions
                }

                String commerciantName = transaction.getCommerciant();
                if (commerciantName == null || commerciantName.isEmpty()) {
                    continue; // Ignore transactions without a valid commerciant
                }

                // Add the commerciant in the map if it doesn't exist
                commerciantData.putIfAbsent(commerciantName,
                                            context.getObjectMapper().createObjectNode());
                ObjectNode commerciantNode = commerciantData.get(commerciantName);

                // Update the spent amount for the commerciant
                double totalReceived = commerciantNode.has("total received")
                        ? commerciantNode.get("total received").asDouble()
                        : 0;
                commerciantNode.put("total received", totalReceived + transaction.getAmount());

                // Add the manager and employee to the commerciant
                ArrayNode managersArray = commerciantNode.has("managers")
                        ? (ArrayNode) commerciantNode.get("managers")
                        : context.getObjectMapper().createArrayNode();
                ArrayNode employeesArray = commerciantNode.has("employees")
                        ? (ArrayNode) commerciantNode.get("employees")
                        : context.getObjectMapper().createArrayNode();

                String fullName = user.getLastName() + " " + user.getFirstName();

                // Add the manager or employee to the corresponding array each time they appear
                if ("manager".equals(role)) {
                    managersArray.add(fullName);
                } else if ("employee".equals(role)) {
                    employeesArray.add(fullName);
                }

                commerciantNode.set("managers", managersArray);
                commerciantNode.set("employees", employeesArray);
            }
        }

        // Add the commerciants to the report node
        ArrayNode commerciantsArray = context.getObjectMapper().createArrayNode();
        for (Map.Entry<String, ObjectNode> entry : commerciantData.entrySet()) {
            ObjectNode commerciantNode = entry.getValue();
            commerciantNode.put("commerciant", entry.getKey());
            commerciantsArray.add(commerciantNode);
        }

        reportNode.set("commerciants", commerciantsArray);
    }
}
