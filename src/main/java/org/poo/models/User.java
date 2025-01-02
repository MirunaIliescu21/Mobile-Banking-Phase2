package org.poo.models;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Data;
import org.poo.commands.CommandContext;
import org.poo.exceptions.CurrencyConversionException;
import org.poo.services.*;

import java.time.LocalDate;
import java.time.Period;
import java.util.*;

@Data
/**
 * The User class represents a user of the banking application.
 * A user has a first name, a last name, an email, a list of accounts and a list of transactions.
 */
public class User {
    private final String firstName;
    private final String lastName;
    private final String email;
    private final List<Account> accounts;
    private List<Transaction> transactions;
    private final String birthDate;
    private final String occupation;
    private ServicePlan currentPlan;
    private Set<String> receivedCashbacks = new HashSet<>(); // Ex. "Food", "Clothes", "Tech"
    private double totalSpending;

    public User(final String firstName, final String lastName, final String email
            , final String birthDate, final String occupation) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.accounts = new ArrayList<>();
        transactions = new ArrayList<>();
        this.birthDate = birthDate;
        this.occupation = occupation;
        // Set the default plan based on the occupation
        currentPlan = occupation.equals("student") ? new StudentPlan() : new StandardPlan();
        totalSpending = 0;
    }

    /**
     * Add a new account to the user.
     * @param account the specific account to be added
     */
    public void addAccount(final Account account) {
        accounts.add(account);
    }

    /**
     * Find a specific account by its IBAN.
     * @param iban the IBAN of the account
     * @return  the account with the specified IBAN or null if it does not exist
     */
    public Account findAccountByIban(final String iban) {
        for (Account account : accounts) {
            if (account.getIban().equals(iban)) {
                return account;
            }
        }
        return null;
    }

    /**
     * Finds the user associated with an account.
     *
     * @param users   the list of users
     * @param account the account to find the owner for
     * @return the user who owns the account
     */
    public static User findUserByAccount(final List<User> users, final Account account) {
        for (User user : users) {
            if (user.getAccounts().contains(account)) {
                return user;
            }
        }
        return null;
    }

    /**
     * Find a specific user by their email.
     * @param users the list of users
     * @param email the email of the user to be found
     * @return the user with the specified email or null if it does not exist
     */
    public static User findUserByEmail(final List<User> users, final String email) {
        for (User user : users) {
            if (user.getEmail().equals(email)) {
                return user;
            }
        }
        return null;
    }

    /**
     * Find a specific account by its IBAN.
     * @param users the list of users
     * @param iban the IBAN of the account to be found
     * @return the account with the specified IBAN or null if it does not exist
     */
    public static Account findAccountByIBAN(final List<User> users, final String iban) {
        for (User user : users) {
            for (Account account : user.getAccounts()) {
                if (account.getIban().equals(iban)) {
                    return account;
                }
            }
        }
        return null;
    }

    /**
     * Check if the user has an account with the specified alias.
     * @param alias the alias of the account
     * @return true if the user has an account with the specified alias, false otherwise
     */
    public boolean hasAlias(final String alias) {
        for (Account account : accounts) {
            if (alias.equals(account.getAlias())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Add a new transaction to the account.
     * @param transaction the specific transaction to be added
     */
    public void addTransaction(final Transaction transaction) {
        transactions.add(transaction);
    }

    /**
     * Print all the transactions of the user.
     * @param transactionsArray the array of transactions
     * @param output the output array
     */
    public void printTransactions(final ArrayNode transactionsArray, final ArrayNode output) {
        for (Transaction transaction : transactions) {
            // Searching for the current transaction
            String transactionType = determineTransactionType(transaction);

            // Creating a JSON object for the transaction
            ObjectNode transactionJson = output.objectNode();
            transactionJson.put("timestamp", transaction.getTimestamp());
            transactionJson.put("description", transaction.getDescription());

            // Processing the transaction using handlers
            TransactionExecutor.executeTransaction(transactionType, transaction, transactionJson);

            // Adding the transaction to the transactions array for the user
            transactionsArray.add(transactionJson);
        }
    }

    /**
     * Determine the type of the transaction.
     * @param transaction the specific transaction
     * @return the type of the transaction
     */
    private String determineTransactionType(final Transaction transaction) {
        if (transaction.getSenderIBAN() != null && transaction.getReceiverIBAN() != null) {
            return "bankTransfer";
        } else if (transaction.getCard() != null && transaction.getCardHolder() != null
                   && transaction.getDescription().equalsIgnoreCase("New card created")) {
            return "createCard";
        } else if (transaction.getDescription().equalsIgnoreCase("Insufficient funds")) {
            return "insufficientFunds";
        } else if (transaction.getDescription().equalsIgnoreCase("New account created")) {
            return "createAccount";
        } else if (transaction.getDescription().equalsIgnoreCase("The card has been destroyed")) {
            return "destroyCard";
        } else if (transaction.getDescription().equalsIgnoreCase("Card payment")) {
            return "cardPayment";
        } else if (transaction.getDescription().equalsIgnoreCase("Interest rate income")) {
            return "addInterest";
        } else if (transaction.getInvolvedAccounts() != null) {
            return "splitPayment";
        } else if (transaction.getCurrentPlan() != null) {
            return "upgradePlan";
        } else if (transaction.getAmount() != 0 && transaction.getError().equalsIgnoreCase("Cash withdrawal")) {
            return "cashWithdrawal";
        }
        return "unknown";
    }

    /**
     * Print the transactions of the user within a specified time interval.
     * @param transactionsArray the array of transactions
     * @param output the output array
     * @param startTimestamp the start timestamp
     * @param endTimestamp the end timestamp
     */
    public void printReportTransactions(final ArrayNode transactionsArray,
                                        final ArrayNode output,
                                        final int startTimestamp,
                                        final int endTimestamp,
                                        final String accountIban) {
        int prevTimestamp = 0;
        for (Transaction transaction : transactions) {
            // Checking if the transaction is for the user's account
            if (!transaction.getAccount().equals(accountIban)) {
                continue;
            }
            // Checking if the transaction is within the specified time interval
            if (transaction.getTimestamp() < startTimestamp) {
                continue;
            }

            if (transaction.getTimestamp() > endTimestamp) {
                break;
            }

            // Checking if the transaction is a duplicate
            if (prevTimestamp != 0 && prevTimestamp == transaction.getTimestamp()) {
                continue;
            }

            // Searching for the current transaction
            String transactionType = determineTransactionType(transaction);

            // Creating a JSON object for the transaction
            ObjectNode transactionJson = output.objectNode();
            transactionJson.put("timestamp", transaction.getTimestamp());
            transactionJson.put("description", transaction.getDescription());

            // Processing the transaction using handlers
            TransactionExecutor.executeTransaction(transactionType, transaction, transactionJson);

            // Adding the transaction to the transactions array for the user
            transactionsArray.add(transactionJson);

            // Updating the previous timestamp
            prevTimestamp = transaction.getTimestamp();
        }
    }

    /**
     * Filter the transactions of the user by type, account and within a specified time interval.
     * @param transactionsArray the array of transactions
     * @param spendings the map of spendings by commerciant
     * @param startTimestamp the start timestamp
     * @param endTimestamp the end timestamp
     * @param objectMapper the object mapper
     * @param accountIban the IBAN of the user's account
     */
    public void filterTransactionsByTypeAndInterval(final ArrayNode transactionsArray,
                                                    final Map<String, Double> spendings,
                                                    final int startTimestamp,
                                                    final int endTimestamp,
                                                    final ObjectMapper objectMapper,
                                                    final String accountIban) {
        for (Transaction transaction : transactions) {
            // Check if the transaction is within the specified time interval
            if (transaction.getTimestamp() < startTimestamp) {
                continue;
            }
            if (transaction.getTimestamp() > endTimestamp) {
                break;
            }

            // Search for the commerciant only for the "Card payment" transactions
            if ("Card payment".equals(transaction.getDescription())) {
                // Check if the transaction is for the user's account
                if (!transaction.getAccount().equals(accountIban)) {
                    continue;
                }
                String commerciant = transaction.getCommerciant();
                double amount = transaction.getAmount();
                // Create a JSON object for the transaction and add it to the transactions array
                ObjectNode transactionJson = objectMapper.createObjectNode();
                transactionJson.put("timestamp", transaction.getTimestamp());
                transactionJson.put("description", transaction.getDescription());
                transactionJson.put("amount", amount);
                transactionJson.put("commerciant", commerciant);
                transactionsArray.add(transactionJson);

                // Calculate the total of spendings by commerciant
                spendings.put(commerciant,
                        spendings.getOrDefault(commerciant, 0.0) + amount);
            }
        }
    }

    /**
     * Check if the user is of minimum age.
     * @param minimumAge the minimum age
     * @return true if the user is of minimum age, false otherwise
     */
    public boolean isOfMinimumAge(final int minimumAge) {
        LocalDate birthDate = LocalDate.parse(this.getBirthDate());
        return Period.between(birthDate, LocalDate.now()).getYears() >= minimumAge;
    }

    /**
     * Get the number of transactions for a specific user's account.
     * @param accountIban the IBAN of the user's account
     * @param minAmount the minimum amount of the transaction
     * @return the number of transactions.
     */
    public int countCardPaymentTransaction(final String accountIban, final double minAmount, final CommandContext context) {
        int count = 0;
        for (Transaction transaction : transactions) {
            if (!transaction.getAccount().equals(accountIban)) {
                continue;
            }

            if (!transaction.getDescription().equals("Card payment")) {
                continue;
            }

            double convertedAmount;
            try {
                convertedAmount = context.getCurrencyConverter().convertCurrency(minAmount,
                        "RON", transaction.getAmountCurrency());
            } catch (CurrencyConversionException e) {
                return -1;
            }

            if (transaction.getAmount() >= convertedAmount) {
                count++;
            }
        }
        System.out.println("S-au efectuat " + count + " tranzactii de tip card payment cu suma minima de " + minAmount + " RON.");
        return count;
    }

    public int getTransactionCountByCommerciant(final Commerciant commerciant, final List<Transaction> transactions) {
        System.out.println("Calculating transaction count for commerciant: " + commerciant.getName());
        int count = 0;
        for (Transaction transaction : transactions) {
            System.out.println("Transaction commerciant: " + transaction.getCommerciant());
            if (transaction.getCommerciant() == null) {
                continue;
            }

            if (transaction.getCommerciant().equals(commerciant.getName())) {
                count++;
            }
        }
        System.out.println("Transaction count: " + count);
        return count;
    }

    // Verifică dacă utilizatorul a primit cashback pentru o categorie specifică
    public boolean hasReceivedCashback(String category) {
        return receivedCashbacks.contains(category);
    }

    // Marchează categoria ca fiind primită
    public void setCashbackReceived(String category) {
        receivedCashbacks.add(category);
    }
}
