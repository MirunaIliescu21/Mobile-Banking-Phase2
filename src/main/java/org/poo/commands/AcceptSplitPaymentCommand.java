package org.poo.commands;

import org.poo.fileio.CommandInput;
import org.poo.models.PaymentProcessor;
import org.poo.models.User;
import static org.poo.commands.CommandErrors.addError;
public class AcceptSplitPaymentCommand implements Command {
    @Override
    /**
     * Executes the businessReport command.
     *
     * @param command the command input containing details for execution
     * @param context the context of the command, including users and services
     */
    public void execute(final CommandInput command, final CommandContext context) {
        System.out.println("acceptSplitPayment " + command.getTimestamp()
                            + " for account " + command.getEmail());
        try {
            if (command.getEmail() == null) {
                throw new IllegalArgumentException("User not found");
            }
            User user = User.findUserByEmail(context.getUsers(), command.getEmail());
            if (user == null) {
                throw new IllegalArgumentException("User not found");
            }
        } catch (IllegalArgumentException e) {
            System.out.println("User not found");
            addError(context.getOutput(), e.getMessage(), command.getTimestamp(), command.getCommand());
            return;
        }
        PaymentProcessor paymentProcessor = PaymentProcessor.getInstance();
        paymentProcessor.processResponse(command.getEmail(), true, context, command.getSplitPaymentType());
    }

}
