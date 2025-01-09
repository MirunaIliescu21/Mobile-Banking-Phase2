package org.poo.commands;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.poo.exceptions.UserNotFoundException;
import org.poo.fileio.CommandInput;
import org.poo.models.User;

import static org.poo.commands.CommandErrors.addError;
import static org.poo.models.User.findUserByEmail;

public class PrintTransactionsCommand implements Command {
    /**
     * Print the transactions of a specific user.
     * @param command   Command input containing email, card number, amount, and currency.
     */
    @Override
    public void execute(final CommandInput command,
                        final CommandContext context) {
        System.out.println(command.getCommand() + " " + command.getTimestamp());
        User user = findUserByEmail(context.getUsers(), command.getEmail());
        try {
            if (user == null) {
                throw new UserNotFoundException("User not found");
            }
        } catch (UserNotFoundException e) {
            addError(context.getOutput(), e.getMessage(),
                    command.getTimestamp(), "history");
            return;
        }

        ArrayNode transactionsArray = context.getObjectMapper().createArrayNode();
        System.out.println("Transactions for user " + user.getEmail());
        user.printTransactions(transactionsArray, context.getOutput());

        ObjectNode commandNode = context.getObjectMapper().createObjectNode();
        commandNode.put("command", "printTransactions");
        commandNode.set("output", transactionsArray);
        commandNode.put("timestamp", command.getTimestamp());
        context.getOutput().add(commandNode);
    }
}
