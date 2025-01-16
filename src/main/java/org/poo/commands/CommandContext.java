package org.poo.commands;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.Getter;
import org.poo.models.User;
import org.poo.services.Commerciant;
import org.poo.services.CurrencyConverter;

import java.util.List;

@Getter
/**
 * CommandContext encapsulates all the resources needed for executing commands:
 * - List of users
 * - ObjectMapper for JSON serialization
 * - ArrayNode for output
 * - CurrencyConverter for currency conversion
 *  - List of commerciants (added in the second part of the project)
 */
public class CommandContext {
    private final List<User> users;
    private final List<Commerciant> commerciants;
    private final ObjectMapper objectMapper;
    private final ArrayNode output;
    private final CurrencyConverter currencyConverter;

    public CommandContext(final List<User> users,
                          final List<Commerciant> commerciants,
                          final ObjectMapper objectMapper,
                          final ArrayNode output,
                          final CurrencyConverter currencyConverter) {
        this.users = users;
        this.commerciants = commerciants;
        this.objectMapper = objectMapper;
        this.output = output;
        this.currencyConverter = currencyConverter;
    }

    /**
     * Find a cmmerciant by name
     * @param name Name of the user
     * @return User with the given name
     */
    public Commerciant findCommerciantByName(final String name) {
        for (Commerciant commerciant : commerciants) {
            if (commerciant.getName().equalsIgnoreCase(name)) {
                return commerciant;
            }
        }
        throw new IllegalArgumentException("Commerciant with name " + name + " not found");
    }

    /**
     * Find a commerciant by IBAN
     * @param iban IBAN of the commerciant
     * @return Commerciant with the given IBAN or null if not found
     */
    public Commerciant findCommerciantByIban(final String iban) {
        for (Commerciant commerciant : commerciants) {
            if (commerciant.getAccount().equalsIgnoreCase(iban)) {
                return commerciant;
            }
        }
        return null;
    }
}
