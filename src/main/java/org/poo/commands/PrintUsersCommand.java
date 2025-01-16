package org.poo.commands;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.poo.fileio.CommandInput;
import org.poo.models.Account;
import org.poo.models.Card;
import org.poo.models.User;

public class PrintUsersCommand implements Command {
    /**
     * Print the users and their accounts to the output file in format JSON.
     * @param command the command to be executed
     * @param context the context of the command
     */
    @Override
    public void execute(final CommandInput command, final CommandContext context) {
        ArrayNode userOutput = context.getObjectMapper().createArrayNode();
        for (User user : context.getUsers()) {
            ObjectNode userNode = context.getObjectMapper().createObjectNode();
            userNode.put("firstName", user.getFirstName());
            userNode.put("lastName", user.getLastName());
            userNode.put("email", user.getEmail());

            ArrayNode accountsNode = context.getObjectMapper().createArrayNode();
            for (Account account : user.getAccounts()) {
                ObjectNode accountNode = context.getObjectMapper().createObjectNode();
                accountNode.put("IBAN", account.getIban());
                accountNode.put("balance", account.getBalance());
                accountNode.put("currency", account.getCurrency());
                accountNode.put("type", account.getType());

                ArrayNode cardsNode = context.getObjectMapper().createArrayNode();
                for (Card card : account.getCards()) {
                    ObjectNode cardNode = context.getObjectMapper().createObjectNode();
                    cardNode.put("cardNumber", card.getCardNumber());
                    cardNode.put("status", card.getStatus());
                    cardsNode.add(cardNode);
                }
                accountNode.set("cards", cardsNode);
                accountsNode.add(accountNode);
            }
            userNode.set("accounts", accountsNode);
            userOutput.add(userNode);
        }
        ObjectNode commandNode = context.getObjectMapper().createObjectNode();
        commandNode.put("command", "printUsers");
        commandNode.set("output", userOutput);
        commandNode.put("timestamp", command.getTimestamp());
        context.getOutput().add(commandNode);
    }
}
