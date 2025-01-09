package org.poo.models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;
import org.poo.commands.CommandContext;
import org.poo.exceptions.CurrencyConversionException;

import static org.poo.commands.CommandErrors.addError;

@Data
public class SplitPayment {
    private static List<String> acceptedResponsesCustom = new ArrayList<>();
    private static List<String> acceptedResponsesEqual = new ArrayList<>();

    private final String splitPaymentType; // Type of split payment ("equal" or "custom")
    private final List<String> accounts; // List of IBANs involved in the split payment
    private final List<Double> amountsForUsers; // Amounts assigned to each user (for "custom" split type)
    private final String currency; // Currency of the payment
    private final double amount; // Total amount of the split payment
    private final int timestamp; // Timestamp of the payment creation
    private final Map<String, Boolean> responses = new HashMap<>(); // Tracks user responses
    private boolean isCompleted = false; // Status of payment completion
    private final Map<String, String> emailToAccount; // Maps user emails to their IBANs
    private final Map<String, Double> accountBalances; // Maps IBANs to their current balances

    // Constructor
    public SplitPayment(final String splitPaymentType, final List<String> accounts,
                        final List<Double> amountsForUsers, final String currency,
                        final double amount, final int timestamp,
                        final Map<String, String> emailToAccount,
                        final Map<String, Double> accountBalances) {
        this.splitPaymentType = splitPaymentType;
        this.accounts = accounts;
        this.amountsForUsers = amountsForUsers;
        this.currency = currency;
        this.amount = amount;
        this.timestamp = timestamp;

        // Initialize responses for all accounts to null (unprocessed)
        for (String account : accounts) {
            responses.put(account, null);
        }
        this.emailToAccount = emailToAccount;
        this.accountBalances = accountBalances;
    }

    /**
     * Retrieves the accepted responses for a given split payment type.
     *
     * @param splitPaymentType The type of the split payment ("equal" or "custom").
     * @return List of accepted responses for the split type.
     */
    public List<String> getAcceptedResponsesForType(String splitPaymentType) {
        if (splitPaymentType.equals("custom")) {
            return acceptedResponsesCustom;
        } else {
            return acceptedResponsesEqual;
        }
    }

    /**
     * Processes a user's response to the split payment.
     *
     * @param account The IBAN of the responding user.
     * @param isAccepted Whether the user accepts the payment.
     * @param context The command context.
     * @return A result string indicating the status of the payment.
     */
    public String processResponse(final String account,
                                  final boolean isAccepted,
                                  final CommandContext context) {
        if (!accounts.contains(account)) {
            return "One of the accounts is invalid.";
        }

        if (GlobalResponseTracker.hasResponded(account, this)) {
            System.out.println("The account has already responded.");
        }

        // Record the global response
        GlobalResponseTracker.addResponse(account, this, isAccepted);

        // If a user rejects the payment, mark it as completed and reject it
        if (!isAccepted) {
            isCompleted = true;
            rejectPayment(context);
            return "One user rejected the payment.";
        }

        // Log responses of all accounts for debugging
        System.out.println("Current responses status:");
        GlobalResponseTracker.getResponsesForPayment(this).forEach((acc, response) -> {
            System.out.println("Account: " + acc + ", Response: " + response);
        });

        // Check if all users have accepted the payment
        boolean allAccepted = accounts.stream()
                .allMatch(acc -> Boolean.TRUE.equals(GlobalResponseTracker.getResponse(acc, this)));

        if (allAccepted) {
            processSplitPayment(context);
            return "Split payment of " + amount + " " + currency + " completed successfully.";
        }

        System.out.println("Payment not completed yet at account: " + account);
        return null;
    }

    /**
     * Handles the rejection of the payment, creating error transactions.
     * @param context The command context.
     */
    private void rejectPayment(final CommandContext context) {
        System.out.println("Reject split payment at " + timestamp);

        System.out.println("splitPaymentType: " + splitPaymentType);
        System.out.println("accounts: " + accounts.size());
        for (int i = 0; i < accounts.size(); i++) {
            String iban = accounts.get(i);
            System.out.println("iban: " + iban);
            double amountForUser;
            if (splitPaymentType.equals("equal")) {
                System.out.println("total amount: " + amount + " " + currency);
                amountForUser = amount / accounts.size();
            } else {
                amountForUser = amountsForUsers.get(i);
            }
            System.out.println("Account: " + iban + " Amount: " + amountForUser);
            Account account = User.findAccountByIBAN(context.getUsers(), iban);
            if (account == null) {
                addError(context.getOutput(), "Account not found: " + iban, timestamp,
                        "processSplitPayment");
                return;
            }

            User user = User.findUserByAccount(context.getUsers(), account);
            if (user == null) {
                return;
            }

            // Add a transaction to the user
            Transaction rejectTransaction = new Transaction.TransactionBuilder(timestamp,
                    "Split payment of " + String.format("%.2f", amount) + " " + currency,
                    account.getIban(), "error")
                    .error("One user rejected the payment.")
                    .splitPaymentType(splitPaymentType)
                    .amount(amountForUser)
                    .amounts(amountsForUsers)
                    .amountCurrency(currency)
                    .involvedAccounts(accounts)
                    .build();
            user.addTransaction(rejectTransaction);
        }
        isCompleted = true;
    }

    /**
     * Processes the split payment, deducting amounts from each account.
     * @param context The command context.
     */
    private void processSplitPayment(final CommandContext context) {
        System.out.println("Processing split payment at " + timestamp);
        // Check if the accounts exist and have enough funds
        List<Account> validAccounts = new ArrayList<>();
        List<Account> invalidAccounts = new ArrayList<>();
        String lastInvalidIban = null;

        System.out.println("splitPaymentType: " + splitPaymentType);
        System.out.println("accounts: " + accounts.size());
        for (int i = 0; i < accounts.size(); i++) {
            String iban = accounts.get(i);
            System.out.println("iban: " + iban);
             double amountForUser;
            if (splitPaymentType.equals("equal")) {
                System.out.println("total amount: " + amount + " " + currency);
                // amountsForUsers.set(i, amount / accounts.size());
                amountForUser = amount / accounts.size();
            } else {
                amountForUser = amountsForUsers.get(i);
            }
            System.out.println("Account: " + iban + " Amount: " + amountForUser);
            Account account = User.findAccountByIBAN(context.getUsers(), iban);
            if (account == null) {
                addError(context.getOutput(), "Account not found: "
                        + iban, timestamp, "processSplitPayment");
                return;
            }

            try {
                // Convert the amount to the account currency
                double convertedAmount;
                convertedAmount = context.getCurrencyConverter().convertCurrency(amountForUser,
                                                                currency, account.getCurrency());
                // Check if the account has enough funds
                if (account.getBalance() - convertedAmount <= account.getMinimumBalance()) {
                    System.out.println("Account " + account.getIban()
                            + " has insufficient funds for a split payment.");
                    lastInvalidIban = account.getIban();
                    invalidAccounts.add(account);
                } else {
                    validAccounts.add(account);
                }
            } catch (CurrencyConversionException e) {
                addError(context.getOutput(),
                        "Currency conversion not supported for account: " + iban,
                        timestamp, "splitPayment");
                return;
            }
        }

        System.out.println("S-a ajuns la invalidAccounts: " + invalidAccounts.size());
        String firstInvalidIban = invalidAccounts.isEmpty()
                                ? null : invalidAccounts.getFirst().getIban();
        if (!invalidAccounts.isEmpty()) {
            for (int i = 0; i < accounts.size(); i++) {
                String iban = accounts.get(i);
                Account involvedAccount = User.findAccountByIBAN(context.getUsers(), iban);
                if (involvedAccount != null) {
                    double amountForUser;
                    if (splitPaymentType.equals("equal")) {
                        System.out.println("total amount: " + amount + " " + currency);
                        amountForUser = amount / accounts.size();
                    } else {
                        amountForUser = amountsForUsers.get(i);
                    }
                    // Add a transaction to the user
                    Transaction errorTransaction = new Transaction.TransactionBuilder(timestamp,
                            "Split payment of " + String.format("%.2f", amount) + " " + currency,
                            involvedAccount.getIban(), "error")
                            .error("Account " + firstInvalidIban
                                    + " has insufficient funds for a split payment.")
                            .splitPaymentType(splitPaymentType)
                            .amount(amountForUser)
                            .amounts(amountsForUsers)
                            .amountCurrency(currency)
                            .involvedAccounts(accounts)
                            .build();
                    User user = User.findUserByAccount(context.getUsers(), involvedAccount);
                    if (user == null) {
                        return;
                    }
                    user.addTransaction(errorTransaction);
                }
            }
            isCompleted = true;
        }

        if (validAccounts.size() != accounts.size()) {
            return;
        }

        // Make the payment for each account if all accounts have enough funds
        System.out.println("S-a ajuns la validAccounts: " + validAccounts.size());
        for (int i = 0; i < validAccounts.size(); i++) {
            Account account = validAccounts.get(i);
            // double amountForUser = amountsForUsers.get(i);
            double amountForUser = 0;
            if (splitPaymentType.equals("equal")) {
                System.out.println("total amount: " + amount + " " + currency);
                // amountsForUsers.set(i, amount / accounts.size());
                amountForUser = amount / accounts.size();
            } else {
                System.out.println("amountsForUsers: " + amountsForUsers.get(i));
                amountForUser = amountsForUsers.get(i);
            }
            User user = User.findUserByAccount(context.getUsers(), account);
            if (user == null) {
                return;
            }
            double convertedAmount;
            try {
                convertedAmount = context.getCurrencyConverter().convertCurrency(amountForUser,
                        currency, account.getCurrency());
            } catch (CurrencyConversionException e) {
                addError(context.getOutput(),
                        "Currency conversion not supported for account: " + account.getIban(),
                        timestamp, "splitPayment");
                return;
            }

            account.setBalance(account.getBalance() - convertedAmount);
            // Add a transaction to the user
            System.out.println("Se adauga tranzacatie pentru userul " + user.getEmail() + " cu accountul " + account.getIban());
            Transaction successTransaction = new Transaction.TransactionBuilder(timestamp,
                    "Split payment of " + String.format("%.2f", amount) + " " + currency,
                    account.getIban(), "spending")
                    .error(null)
                    .splitPaymentType(splitPaymentType)
                    .amount(amountForUser)
                    .amounts(amountsForUsers)
                    .amountCurrency(currency)
                    .involvedAccounts(accounts)
                    .build();
            user.addTransaction(successTransaction);
        }
        isCompleted = true;
    }
}
