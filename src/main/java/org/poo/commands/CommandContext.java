package org.poo.commands;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.Getter;
import org.poo.models.User;
import org.poo.services.CurrencyConverter;

import java.util.List;

@Getter
/**
 * CommandContext encapsulates all the resources needed for executing commands:
 * - List of users
 * - ObjectMapper for JSON serialization
 * - ArrayNode for output
 * - CurrencyConverter for currency conversion
 */
public class CommandContext {
    private final List<User> users;
    private final ObjectMapper objectMapper;
    private final ArrayNode output;
    private final CurrencyConverter currencyConverter;

    public CommandContext(final List<User> users,
                          final ObjectMapper objectMapper,
                          final ArrayNode output,
                          final CurrencyConverter currencyConverter) {
        this.users = users;
        this.objectMapper = objectMapper;
        this.output = output;
        this.currencyConverter = currencyConverter;
    }
}
