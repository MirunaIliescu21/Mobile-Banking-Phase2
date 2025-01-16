package org.poo.commands;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.poo.fileio.CommandInput;
import org.poo.models.User;
import org.poo.services.Commerciant;
import org.poo.services.CurrencyConverter;

import java.util.List;

/**
 * Class that executes the commands.
 */
public final class CommandExecutor {

    private final CommandContext context;

    public CommandExecutor(final List<User> users,
                           final List<Commerciant> commerciants,
                           final ObjectMapper objectMapper,
                           final ArrayNode output,
                           final CurrencyConverter currencyConverter) {
        this.context = new CommandContext(users, commerciants, objectMapper,
                                          output, currencyConverter);
    }

    /**
     * Executes the command.
     * @param commandInput the input of the command
     * @throws Exception if the command is not found
     */
    public void execute(final CommandInput commandInput) throws Exception {
        CommandType commandType = CommandType.fromCommandName(commandInput.getCommand());
        Command command = CommandRegistry.getCommand(commandType);
        command.execute(commandInput, context);
    }
}
