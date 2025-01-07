package org.poo.models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;
import org.poo.commands.CommandActions;
import org.poo.commands.CommandContext;
import org.poo.exceptions.CurrencyConversionException;
import org.poo.fileio.CommandInput;

import static org.poo.commands.CommandActions.addError;

@Data
public class SplitPayment {
    private final String splitPaymentType;
    private final List<String> accounts;
    private final List<Double> amountsForUsers;
    private final  String currency;
    private final double amount;
    private final int timestamp;
    private final Map<String, Boolean> responses = new HashMap<>();
    private boolean isCompleted = false;
    private final Map<String, String> emailToAccount;
    private final Map<String, Double> accountBalances;

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
        // Initialize all responses to null (not processed)
        for (String account : accounts) {
            responses.put(account, null);
        }
        this.emailToAccount = emailToAccount;
        this.accountBalances = accountBalances;
    }

    /**
     * Method to process the response of a user
     * @param account The iban of the user
     * @param isAccepted  Whether the user accepted the payment
     * @param accountBalances The balances of the accounts
     * @return a string with the result of the payment
     */
    public String processResponse(String account, boolean isAccepted, Map<String, Double> accountBalances, final CommandContext context) {
        if (!responses.containsKey(account)) {
            return "One of the accounts is invalid.";
        }

        if (responses.get(account) != null) {
            return "The account has already responded.";
        }

        System.out.println(responses.containsKey(account));
        // Update the response
        responses.put(account, isAccepted);

        // If one user rejected the payment, the payment is cancelled
        if (!isAccepted) {
            isCompleted = true;
            System.out.println("One user rejected the payment.");
            rejectPayment(context);
            return "One user rejected the payment.";
        }

        // Print all accounts and their responses
        System.out.println("Current responses status:");
        responses.forEach((acc, response) -> {
            System.out.println("Account: " + acc + ", Response: " + response);
        });

        // Check if all users have responded
        if (responses.values().stream().allMatch(response -> response != null && response)) {
            processSplitPayment(context);
            return "Split payment of " + amount + " " + currency + " completed successfully.";
            // return completePayment(accountBalances);
        }

        // Return null if the payment is not completed
        System.out.println("Payment not completed yet at account: " + account);
        return null;
    }

//    // Make the payment
//    private String completePayment(Map<String, Double> accountBalances) {
//        for (int i = 0; i < accounts.size(); i++) {
//            String account = accounts.get(i);
//            double requiredAmount = amountsForUsers.get(i);
//
//            // Check if the account has enough funds
//            if (accountBalances.getOrDefault(account, 0.0) < requiredAmount) {
//                isCompleted = true;
//                return "Account " + account + " has insufficient funds for a split payment.";
//            }
//        }
//
//        // Update the balances
//        for (int i = 0; i < accounts.size(); i++) {
//            String account = accounts.get(i);
//            double requiredAmount = amountsForUsers.get(i);
//            accountBalances.put(account, accountBalances.get(account) - requiredAmount);
//        }
//
//        isCompleted = true;
//        return "Split payment of " + amount + " " + currency + " completed successfully.";
//    }

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
                addError(context.getOutput(), "Account not found: " + iban, timestamp, "processSplitPayment");
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
                    .amount(CommandActions.roundToTwoDecimals(amountForUser))
                    .amounts(amountsForUsers)
                    .amountCurrency(currency)
                    .involvedAccounts(accounts)
                    .build();
            user.addTransaction(rejectTransaction);
        }
        isCompleted = true;
    }

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
                addError(context.getOutput(), "Account not found: " + iban, timestamp, "processSplitPayment");
                return;
            }

            try {
                // Convert the amount to the account currency
                double convertedAmount;
                convertedAmount = context.getCurrencyConverter().convertCurrency(amountForUser,
                                                                currency, account.getCurrency());
                // Check if the account has enough funds
                if (account.getBalance() - convertedAmount <= account.getMinimumBalance()) {
                    System.out.println("Account " + account.getIban() + " has insufficient funds for a split payment.");
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
        String firstInvalidIban = invalidAccounts.isEmpty() ? null : invalidAccounts.getFirst().getIban();
        if (!invalidAccounts.isEmpty()) {
            for (int i = 0; i < accounts.size(); i++) {
                String iban = accounts.get(i);
                Account involvedAccount = User.findAccountByIBAN(context.getUsers(), iban);
                if (involvedAccount != null) {
                    double amountForUser;
                    if (splitPaymentType.equals("equal")) {
                        System.out.println("total amount: " + amount + " " + currency);
                        // amountsForUsers.set(i, amount / accounts.size());
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
                            .amount(CommandActions.roundToTwoDecimals(amountForUser))
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
                    .amount(CommandActions.roundToTwoDecimals(amountForUser))
                    .amounts(amountsForUsers)
                    .amountCurrency(currency)
                    .involvedAccounts(accounts)
                    .build();
            user.addTransaction(successTransaction);
        }
        isCompleted = true;
    }

}
