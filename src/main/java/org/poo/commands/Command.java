package org.poo.commands;

import org.poo.fileio.CommandInput;

public interface Command {
    void execute(CommandInput command, CommandContext context) throws Exception;
}
