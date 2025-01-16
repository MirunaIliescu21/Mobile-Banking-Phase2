package org.poo.commands;

import org.poo.fileio.CommandInput;

public interface Command {
    /**
     * Execute the command. Each class that implements this interface should override this method.
     * @param command the command input
     * @param context the command context
     * @throws Exception if an error occurs
     */
    void execute(CommandInput command, CommandContext context) throws Exception;
}
