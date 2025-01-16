package org.poo.models;

import org.poo.commands.CommandContext;

import java.util.ArrayList;
import java.util.List;

public final class PaymentProcessor {
    private static PaymentProcessor instance = null; // Singleton instance

    private List<SplitPayment> commands = new ArrayList<>();

    // Private constructor for the Singleton pattern
    private PaymentProcessor() {
    }

    /**
     * Method to retrieve the singleton instance of PaymentProcessor.
     * Ensures that only one instance of this class exists.
     *
     * @return the singleton instance of PaymentProcessor
     */
    public static PaymentProcessor getInstance() {
        if (instance == null) {
            instance = new PaymentProcessor();
        }
        return instance;
    }

    /**
     * Adds a new SplitPayment command to the list of commands.
     *
     * @param payment the SplitPayment command to be added
     */
    public void addSplitPayment(final SplitPayment payment) {
        commands.add(payment);
    }

    /**
     * Processes a user's response to a split payment.
     *
     * @param email       the email of the user responding
     * @param isAccepted  whether the user accepts the split payment
     * @param context     the CommandContext providing additional details
     * @param paymentType the type of the split payment being processed
     */
    public void processResponse(final String email, final boolean isAccepted,
                                final CommandContext context, final String paymentType) {
        System.out.println("Processing response for email: " + email
                            + " with isAccepted: " + isAccepted);
        System.out.println("Commands: " + commands.size());

        // Iterate through the list of split payment commands
        for (SplitPayment command : commands) {
            System.out.println("Command: " + command.getSplitPaymentType());

            // Skip commands that do not match the specified payment type
            if (!command.getSplitPaymentType().equals(paymentType)) {
                continue;
            }

            // Get the account IBAN associated with the user's email
            String iban = command.getEmailToAccount().get(email);
            if (iban == null) {
                System.out.println("Email not found in command");
                continue;
            }

            // Process the response if the payment is not completed and the account is involved
            if (!command.isCompleted() && command.getAccounts().contains(iban)) {
                String result = command.processResponse(iban, isAccepted, context);
                if (command.isCompleted()) {
                    System.out.println("Command completed");
                    commands.remove(command); // Remove the command if it is completed
                }
                if (result != null) {
                    System.out.println(result); // Print the result if available
                }
            }
        }
        System.out.println("No active split payment found for email: " + email);
    }

    /**
     * Helper function:
     * Prints details of all active (incomplete) split payment commands.
     */
    public void printActiveCommands() {
        for (SplitPayment command : commands) {
            if (!command.isCompleted()) {
                System.out.println("Active splitPayment: "
                        + command.getSplitPaymentType() + " at " + command.getTimestamp());
            }
        }
    }
}


