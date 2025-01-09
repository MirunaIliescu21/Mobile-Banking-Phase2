package org.poo.commands;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Abstract class that contains methods for adding errors to the output array.
 */
public abstract class CommandErrors {

    /**
     * Add an error description to the output array.
     * @param output the output array
     * @param errorMessage the error message
     * @param timestamp the timestamp of the command
     * @param commandName the name of the command
     */
    public static void addError(final ArrayNode output, final String errorMessage,
                                final int timestamp, final String commandName) {
        ObjectNode errorNode = output.addObject();
        errorNode.put("command", commandName);
        ObjectNode descriptionNode = errorNode.putObject("output");
        descriptionNode.put("timestamp", timestamp);
        descriptionNode.put("description", errorMessage);
        errorNode.put("timestamp", timestamp);
    }

    /**
     * Add an output error to the output array.
     * @param output the output array
     * @param errorMessage the error message
     * @param timestamp the timestamp of the command
     * @param commandName the name of the command
     */
    public static void addErrorDescription(final ArrayNode output, final String errorMessage,
                                           final int timestamp, final String commandName) {
        ObjectNode errorNode = output.addObject();
        errorNode.put("command", commandName);
        ObjectNode descriptionNode = errorNode.putObject("output");
        descriptionNode.put("timestamp", timestamp);
        descriptionNode.put("error", errorMessage);
        errorNode.put("timestamp", timestamp);
    }

    /**
     * Add an error type to the output array.
     * @param output the output array
     * @param errorMessage the error message
     * @param timestamp the timestamp of the command
     * @param commandName the name of the command
     */
    public static void addErrorType(final ArrayNode output,
                                    final String errorMessage,
                                    final int timestamp,
                                    final String commandName) {
        ObjectNode errorNode = output.addObject();
        errorNode.put("command", commandName);
        ObjectNode descriptionNode = errorNode.putObject("output");
        descriptionNode.put("error", errorMessage);
        errorNode.put("timestamp", timestamp);
    }
}
