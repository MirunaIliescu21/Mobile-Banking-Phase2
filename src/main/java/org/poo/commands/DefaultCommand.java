package org.poo.commands;

import org.poo.fileio.CommandInput;

public class DefaultCommand implements Command {
    /**
     * Handle the default case when the command is not recognized.
     * @param command the command to be executed
     * @param context the context of the command
     */
    @Override
    public void execute(final CommandInput command, final CommandContext context) {
        System.out.println("Invalid command: " + command.getCommand());
    }
}
