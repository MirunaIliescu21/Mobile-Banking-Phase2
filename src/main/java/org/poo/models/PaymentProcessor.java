package org.poo.models;

import org.poo.commands.CommandContext;

import java.util.*;

public class PaymentProcessor {
    private static PaymentProcessor instance = null; // Singleton instance

    private List<SplitPayment> commands = new ArrayList<>();

    // Constructor privat pentru singleton
    private PaymentProcessor() {
    }

    public static PaymentProcessor getInstance() {
        if (instance == null) {
            instance = new PaymentProcessor();
        }
        return instance;
    }

    public void addSplitPayment(SplitPayment payment) {
        commands.add(payment);
    }

    public void processResponse(String email, boolean isAccepted, final CommandContext context, final String paymentType) {
        System.out.println("Processing response for email: " + email + " with isAccepted: " + isAccepted);
        System.out.println("Commands: " + commands.size());

        for (SplitPayment command : commands) {
            System.out.println("Command: " + command.getSplitPaymentType());
            if (!command.getSplitPaymentType().equals(paymentType)) {
                System.out.println("Procesarea nu este pentru acest tip de comanda");
                continue;
            }

            String iban = command.getEmailToAccount().get(email);
            if (iban == null) {
                System.out.println("Email not found in command");
                continue;
            }
            if (!command.isCompleted() && command.getAccounts().contains(iban)) {
                String result = command.processResponse(iban, isAccepted, command.getSplitPaymentType(), context);
                if (command.isCompleted()) {
                    System.out.println("Command completed");
                    commands.remove(command);
                }
                if (result != null) {
                    System.out.println(result);
                }
                // return;
            }
        }
        System.out.println("No active split payment found for email: " + email);
    }

    public void printActiveCommands() {
        for (SplitPayment command : commands) {
            if (!command.isCompleted()) {
                System.out.println("Active splitPayment: " + command.getSplitPaymentType() + " at " + command.getTimestamp());
            }
        }
    }
}


