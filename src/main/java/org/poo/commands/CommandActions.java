package org.poo.commands;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.poo.exceptions.AccountNotFoundException;
import org.poo.exceptions.CardNotFoundException;
import org.poo.exceptions.CurrencyConversionException;
import org.poo.exceptions.InsufficientFundsException;
import org.poo.exceptions.UnauthorizedCardAccessException;
import org.poo.exceptions.UnauthorizedCardStatusException;
import org.poo.exceptions.UserNotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;

import org.poo.models.Account;
import org.poo.models.Card;
import org.poo.models.Transaction;
import org.poo.models.User;
import org.poo.fileio.CommandInput;
import org.poo.services.*;
import org.poo.utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.poo.models.User.findUserByEmail;

/**
 * Class that contains the actions for each command.
 */
public final class CommandActions {
    private static final CommandActions INSTANCE = new CommandActions();

    private CommandActions() {
    }

    public static CommandActions getInstance() {
        return INSTANCE;
    }

    /**
     * Print the users and their accounts to the output file in format JSON.
     * @param command the command to be executed
     * @param context the context of the command
     */
    public void printUsers(final CommandInput command, final CommandContext context) {
        ArrayNode userOutput = context.getObjectMapper().createArrayNode();
        for (User user : context.getUsers()) {
            ObjectNode userNode = context.getObjectMapper().createObjectNode();
            userNode.put("firstName", user.getFirstName());
            userNode.put("lastName", user.getLastName());
            userNode.put("email", user.getEmail());

            ArrayNode accountsNode = context.getObjectMapper().createArrayNode();
            for (Account account : user.getAccounts()) {
                ObjectNode accountNode = context.getObjectMapper().createObjectNode();
                accountNode.put("IBAN", account.getIban());
                accountNode.put("balance", roundToTwoDecimals(account.getBalance()));
                accountNode.put("currency", account.getCurrency());
                accountNode.put("type", account.getType());

                ArrayNode cardsNode = context.getObjectMapper().createArrayNode();
                for (Card card : account.getCards()) {
                    ObjectNode cardNode = context.getObjectMapper().createObjectNode();
                    cardNode.put("cardNumber", card.getCardNumber());
                    cardNode.put("status", card.getStatus());
                    cardsNode.add(cardNode);
                }
                accountNode.set("cards", cardsNode);
                accountsNode.add(accountNode);
            }
            userNode.set("accounts", accountsNode);
            userOutput.add(userNode);
        }

        ObjectNode commandNode = context.getObjectMapper().createObjectNode();
        commandNode.put("command", "printUsers");
        commandNode.set("output", userOutput);
        commandNode.put("timestamp", command.getTimestamp());
        context.getOutput().add(commandNode);
    }

    /**
     * Add an account to a user.
     * Add a new Transaction to the user.
     * @param command the command to be executed
     */
    public void addAccount(final CommandInput command, final CommandContext context) {
        User user = findUserByEmail(context.getUsers(), command.getEmail());
        try {
            if (user == null) {
                throw new UserNotFoundException("User not found");
            }

            String iban = Utils.generateIBAN();
            user.addAccount(new Account(iban, command.getCurrency(),
                    command.getAccountType(), command.getEmail()));

            Transaction transaction = new Transaction.TransactionBuilder(command.getTimestamp(),
                    "New account created", iban)
                    .build();
            user.addTransaction(transaction);
        } catch (UserNotFoundException e) {
            addError(context.getOutput(), e.getMessage(),
                    command.getTimestamp(), "addAccount");
        }

    }

    /**
     * Create a card for a specific account.
     * Add a new transaction to the user.
     * @param command the command to be executed
     */
    public void createCard(final CommandInput command, final CommandContext context) {
        User user = findUserByEmail(context.getUsers(), command.getEmail());
        if (user != null) {
            Account account = user.findAccountByIban(command.getAccount());
            if (account != null) {
                String cardNumber = Utils.generateCardNumber();
                account.addCard(new Card(cardNumber, "active", "normal"));
                Transaction transaction = new Transaction.TransactionBuilder(command.getTimestamp(),
                        "New card created", account.getIban())
                        .card(cardNumber)
                        .cardHolder(user.getEmail())
                        .build();
                user.addTransaction(transaction);
            }
        }
    }

    /**
     * Create a "one time pay" card for a specific account.
     * Add a new transaction to the user.
     * @param command the command to be executed
     */
    public void createOneTimeCard(final CommandInput command, final CommandContext context) {
        try {
            User user = findUserByEmail(context.getUsers(), command.getEmail());

            if (user == null) {
                throw new UserNotFoundException("User not found");

            }
            Account account = user.findAccountByIban(command.getAccount());
            if (account == null) {
                throw new AccountNotFoundException("Account not found");
            }

            // Generate the card number and create the card
            String cardNumber = Utils.generateCardNumber();
            Card oneTimeCard = new Card(cardNumber, "active", "one time pay");

            account.addCard(oneTimeCard);

            // Add a success transaction to the user
            Transaction transaction = new Transaction.TransactionBuilder(command.getTimestamp(),
                    "New card created", account.getIban())
                    .card(cardNumber)
                    .cardHolder(user.getEmail())
                    .build();
            user.addTransaction(transaction);
        } catch (UserNotFoundException | AccountNotFoundException e) {
            addError(context.getOutput(), e.getMessage(),
                    command.getTimestamp(), "createOneTimeCard");
        }
    }


    /**
     * Add funds to a specific account.
     * @param command the command to be executed
     */
    public void addFunds(final CommandInput command, final CommandContext context) {
        for (User user : context.getUsers()) {
            Account account = user.findAccountByIban(command.getAccount());
            if (account != null) {
                account.addFunds(command.getAmount());
                break;
            }
        }
    }

    /**
     * Deletes an account from a user and sets the status of all the cards to "destroyed".
     * If the account has a balance different from 0, throws an exception.
     * Adds success or error messages to the output as appropriate.
     *
     * @param commandInput the command to be executed
     * @param context the command execution context
     */
    public void deleteAccount(final CommandInput commandInput,
                              final CommandContext context) {
        String accountIban = commandInput.getAccount();
        int timestamp = commandInput.getTimestamp();

        try {
            // Search for the user that has the account with the specified IBAN
            User userWithAccount = null;
            Account accountToDelete = null;

            for (User user : context.getUsers()) {
                accountToDelete = user.findAccountByIban(accountIban);
                if (accountToDelete != null) {
                    userWithAccount = user;
                    break;
                }
            }

            // If the user does not exist, add an error to the output
            if (userWithAccount == null) {
                throw new UserNotFoundException("User not found");
            }

            // Check if the account balance is non-zero
            if (accountToDelete.getBalance() != 0) {
                Transaction transaction = new Transaction.TransactionBuilder(timestamp,
                        "Account couldn't be deleted - there are funds remaining",
                        accountToDelete.getIban())
                        .build();
                userWithAccount.addTransaction(transaction);

                throw new IllegalStateException("Account couldn't be deleted "
                        + "- see org.poo.transactions for details");
            }

            // Set the status of all the cards of the account to "destroyed"
            for (Card card : accountToDelete.getCards()) {
                card.setStatus("destroyed");
            }

            // Remove the account from the user
            userWithAccount.getAccounts().remove(accountToDelete);

            // Add a success message to the output
            ObjectNode successNode = context.getOutput().addObject();
            successNode.put("command", "deleteAccount");
            ObjectNode descriptionNode = successNode.putObject("output");
            descriptionNode.put("success", "Account deleted");
            descriptionNode.put("timestamp", timestamp);
            successNode.put("timestamp", timestamp);

        } catch (UserNotFoundException e) {
            // Handle "User not found" exception
            addError(context.getOutput(), e.getMessage(), timestamp, "deleteAccount");
        } catch (IllegalStateException e) {
            // Handle "Account couldn't be deleted" exception
            addErrorDescription(context.getOutput(), e.getMessage(), timestamp, "deleteAccount");
        }
    }

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
     * Delete a specific card and set its status to "destroyed".
     * Add a new transaction to the user.
     * @param command the command to be executed
     */
    public void deleteCard(final CommandInput command,
                                  final CommandContext context) {
        String email = command.getEmail();
        String cardNumber = command.getCardNumber();
        int timestamp = command.getTimestamp();

        // Find the user with the specified email
        User user = findUserByEmail(context.getUsers(), email);
        if (user == null) {
            addError(context.getOutput(), "User not found", timestamp, "deleteCard");
            return;
        }

        for (Account account : user.getAccounts()) {
            // Search for the card in each account of the user
            Card cardToDelete = account.findCardByNumber(cardNumber);

            if (cardToDelete != null) {
                // Set the status of the card to "destroyed"
                cardToDelete.setStatus("destroyed");
                account.getCards().remove(cardToDelete);
                Transaction transaction = new Transaction.TransactionBuilder(timestamp,
                        "The card has been destroyed", account.getIban())
                        .card(cardNumber)
                        .cardHolder(user.getEmail())
                        .build();
                user.addTransaction(transaction);
                return;
            }
        }
    }

    /**
     * Pay online with a specific card.
     * Add an error to the output if the user or the card is not found, or if the user
     * does not own the card.
     * Convert the payment amount from the specified currency to the account's currency.
     * Check if the account has sufficient funds after considering its minimum balance.
     * Deduct the converted amount from the account balance if all conditions are met.
     * Log the payment as a transaction.
     * If the card is a "one-time pay" card, destroy it after the payment.
     *    - Log the destruction of the card as a transaction.
     *    - Create a new "one-time pay" card and log its creation.
     * Manage Failed Payments:
     *    - If the payment fails due to insufficient funds or other issues,
     *      refund the amount to the account, freeze the card, and log the incident.
     * The method ensures that all operations are logged appropriately through transactions
     * or errors, maintaining a complete audit trail of the process.
     * @param command Command input containing email, card number, amount, and currency.
     */
//    public void payOnline(final CommandInput command,
//                          final CommandContext context) throws UserNotFoundException,
//                                                               CardNotFoundException,
//                                                               UnauthorizedCardAccessException,
//                                                               InsufficientFundsException,
//                                                               UnauthorizedCardStatusException {
//        String email = command.getEmail();
//        String cardNumber = command.getCardNumber();
//        int timestamp = command.getTimestamp();
//
//        User user = findUserByEmail(context.getUsers(), email);
//        Card cardUser = null;
//        Account accountUser = null;
//
//        try {
//            // Check if the user exists
//            if (user == null) {
//                throw new UserNotFoundException("User not found");
//            }
//
//            // Search for the card in each account of the user
//            for (Account account : user.getAccounts()) {
//                if (account.findCardByNumber(cardNumber) != null) {
//                    cardUser = account.findCardByNumber(cardNumber);
//                    accountUser = account;
//                    break;
//                }
//            }
//
//            // If the card was not found, add an error to the output
//            if (cardUser == null) {
//                throw new CardNotFoundException("Card not found");
//            }
//
//            // Check if the user owns the card
//            if (!accountUser.getOwner().equals(email)) {
//                throw new UnauthorizedCardAccessException("User does not own the card");
//            }
//            // Convert the amount to the account currency
//            double amountInAccountCurrency;
//            amountInAccountCurrency = context.getCurrencyConverter().
//                    convertCurrency(command.getAmount(),
//                    command.getCurrency(),
//                    accountUser.getCurrency());
//
//            // Check if the card is active amd if the account has enough funds
//            double newBalance = accountUser.getBalance() - amountInAccountCurrency;
//            if (newBalance < accountUser.getMinimumBalance()) {
//                if (cardUser.getStatus().equals("active")
//                        && amountInAccountCurrency > accountUser.getBalance()) {
//                    throw new InsufficientFundsException("Insufficient funds");
//                }
//            }
//
//            // Make the payment
//            accountUser.setBalance(accountUser.getBalance() - amountInAccountCurrency);
//            if (cardUser.getStatus().equals("active")
//                    && accountUser.getBalance() >= accountUser.getMinimumBalance()) {
//                cardUser.setStatus("active");
//                Transaction transaction = new Transaction.TransactionBuilder(timestamp,
//                        "Card payment", accountUser.getIban())
//                        .amount(amountInAccountCurrency)
//                        .commerciant(command.getCommerciant())
//                        .build();
//                user.addTransaction(transaction);
//
//                // If the card is the type of "one time pay",
//                // it is destroyed after the payment and a new card is created
//                if (cardUser.getType().equals("one time pay")) {
//                    cardUser.setStatus("destroyed");
//                    accountUser.getCards().remove(cardUser);
//                    Transaction transaction1 = new Transaction.TransactionBuilder(timestamp,
//                            "The card has been destroyed", accountUser.getIban())
//                            .card(cardNumber)
//                            .cardHolder(user.getEmail())
//                            .build();
//                    user.addTransaction(transaction1);
//
//                    String newCardNumber = Utils.generateCardNumber();
//                    Card oneTimeCard = new Card(newCardNumber, "active", "one time pay");
//                    accountUser.addCard(oneTimeCard);
//
//                    Transaction transaction2;
//                    transaction2 = new Transaction.TransactionBuilder(command.getTimestamp(),
//                            "New card created", accountUser.getIban())
//                            .card(newCardNumber)
//                            .cardHolder(user.getEmail())
//                            .build();
//                    user.addTransaction(transaction2);
//                }
//            } else {
//                // The payment cannot be made, so the amount is returned to the account
//                // and the card is frozen
//                cardUser.setStatus("frozen");
//                accountUser.setBalance(accountUser.getBalance() + amountInAccountCurrency);
//                throw new UnauthorizedCardStatusException("The card is frozen");
//            }
//        } catch (UserNotFoundException | CardNotFoundException | UnauthorizedCardAccessException
//                 | CurrencyConversionException e) {
//            addError(context.getOutput(), e.getMessage(), timestamp, "payOnline");
//        } catch (InsufficientFundsException | UnauthorizedCardStatusException e) {
//            // Add a transaction to the account
//            Transaction transaction = new Transaction.TransactionBuilder(timestamp,
//                    e.getMessage(), accountUser.getIban()).build();
//            user.addTransaction(transaction);
//        } catch (IllegalArgumentException e) {
//            // Add an error to the output if the currency conversion is not supported
//            addError(context.getOutput(), "Currency conversion not supported",
//                    timestamp, "payOnline");
//        }
//    }
        public void payOnline(final CommandInput command,
                          final CommandContext context) throws UserNotFoundException,
                                                               CardNotFoundException,
                                                               UnauthorizedCardAccessException,
                                                               InsufficientFundsException,
                                                               UnauthorizedCardStatusException {
            System.out.println("payOnline " + command.getTimestamp());
        String email = command.getEmail();
        String cardNumber = command.getCardNumber();
        int timestamp = command.getTimestamp();

        User user = findUserByEmail(context.getUsers(), email);
        Card cardUser = null;
        Account accountUser = null;

        try {
            // Check if the user exists
            if (user == null) {
                throw new UserNotFoundException("User not found");
            }

            // Search for the card in each account of the user
            for (Account account : user.getAccounts()) {
                if (account.findCardByNumber(cardNumber) != null) {
                    cardUser = account.findCardByNumber(cardNumber);
                    accountUser = account;
                    break;
                }
            }

            // If the card was not found, add an error to the output
            if (cardUser == null) {
                throw new CardNotFoundException("Card not found");
            }

            // Check if the user owns the card
            if (!accountUser.getOwner().equals(email)) {
                throw new UnauthorizedCardAccessException("User does not own the card");
            }
            // Convert the amount to the account currency
            double amountInAccountCurrency;
            amountInAccountCurrency = context.getCurrencyConverter().
                    convertCurrency(command.getAmount(),
                    command.getCurrency(),
                    accountUser.getCurrency());

            // Calculate commission
            double commission = user.getCurrentPlan().calculateTransactionFee(amountInAccountCurrency);
            System.out.println("commission: " + commission);
            // Calculate cashback
            Commerciant commerciant = context.findCommerciantByName(command.getCommerciant());
            System.out.println("commerciant: " + commerciant.getName() + " " + commerciant.getType());

            CashbackStrategy cashbackStrategy = commerciant.getCashbackStrategyInstance();
            double cashback = cashbackStrategy.calculateCashback(user, commerciant, amountInAccountCurrency);
            System.out.println("cashback: " + cashback);

            // Apply commission and cashback
            double finalAmount = amountInAccountCurrency + commission - cashback;
            System.out.println("finalAmount: " + finalAmount);

            // Check if the card is active amd if the account has enough funds
            double newBalance = accountUser.getBalance() - finalAmount;
            if (newBalance < accountUser.getMinimumBalance()) {
                if (cardUser.getStatus().equals("active")
                        && finalAmount > accountUser.getBalance()) {
                    throw new InsufficientFundsException("Insufficient funds");
                }
            }

            // Make the payment
            accountUser.setBalance(accountUser.getBalance() - finalAmount);
            if (cardUser.getStatus().equals("active")
                    && accountUser.getBalance() >= accountUser.getMinimumBalance()) {
                cardUser.setStatus("active");
                Transaction transaction = new Transaction.TransactionBuilder(timestamp,
                        "Card payment", accountUser.getIban())
                        .amount(amountInAccountCurrency)
                        .commerciant(command.getCommerciant())
                        .build();
                user.addTransaction(transaction);

                // If the card is the type of "one time pay",
                // it is destroyed after the payment and a new card is created
                if (cardUser.getType().equals("one time pay")) {
                    cardUser.setStatus("destroyed");
                    accountUser.getCards().remove(cardUser);
                    Transaction transaction1 = new Transaction.TransactionBuilder(timestamp,
                            "The card has been destroyed", accountUser.getIban())
                            .card(cardNumber)
                            .cardHolder(user.getEmail())
                            .build();
                    user.addTransaction(transaction1);

                    String newCardNumber = Utils.generateCardNumber();
                    Card oneTimeCard = new Card(newCardNumber, "active", "one time pay");
                    accountUser.addCard(oneTimeCard);

                    Transaction transaction2;
                    transaction2 = new Transaction.TransactionBuilder(command.getTimestamp(),
                            "New card created", accountUser.getIban())
                            .card(newCardNumber)
                            .cardHolder(user.getEmail())
                            .build();
                    user.addTransaction(transaction2);
                }
            } else {
                // The payment cannot be made, so the amount is returned to the account
                // and the card is frozen
                cardUser.setStatus("frozen");
                accountUser.setBalance(accountUser.getBalance() + finalAmount);
                throw new UnauthorizedCardStatusException("The card is frozen");
            }
        } catch (UserNotFoundException | CardNotFoundException | UnauthorizedCardAccessException
                 | CurrencyConversionException e) {
            addError(context.getOutput(), e.getMessage(), timestamp, "payOnline");
        } catch (InsufficientFundsException | UnauthorizedCardStatusException e) {
            // Add a transaction to the account
            Transaction transaction = new Transaction.TransactionBuilder(timestamp,
                    e.getMessage(), accountUser.getIban()).build();
            user.addTransaction(transaction);
        } catch (IllegalArgumentException e) {
            // Add an error to the output if the currency conversion is not supported
            addError(context.getOutput(), "Currency conversion not supported",
                    timestamp, "payOnline");
        }
    }

    /**
     * Send money from one account to another.
     * Add a transaction to the sender and the receiver.
     * @param command Command input containing email, card number, amount, and currency.
     * @param context Command context containing the list of users and the currency converter.
     */
    public void sendMoney(final CommandInput command,
                          final CommandContext context)
            throws AccountNotFoundException, UserNotFoundException, InsufficientFundsException {
        String senderIBAN = command.getAccount();
        String receiverIBAN = command.getReceiver();
        double amount = command.getAmount();
        String senderEmail = command.getEmail();

        // Verify if the sender and the receiver accounts exist
        Account senderAccount = User.findAccountByIBAN(context.getUsers(), senderIBAN);
        Account receiverAccount = User.findAccountByIBAN(context.getUsers(), receiverIBAN);

        User senderUser = null;

        try {
            if (senderAccount == null || receiverAccount == null) {
                throw new AccountNotFoundException("One or both accounts not found.");
            }

            senderUser = findUserByEmail(context.getUsers(), senderEmail);
            User receiverUser = findUserByEmail(context.getUsers(), receiverAccount.getOwner());
            if (senderUser == null || receiverUser == null) {
                throw new UserNotFoundException("One or both users not found.");
            }

            // Make the actual transaction
            makeTransaction(senderAccount, receiverAccount, senderUser, receiverUser,
                            senderIBAN, receiverIBAN, amount, command, context);
        } catch (InsufficientFundsException e) {
            // Add a transaction to the sender's account
            Transaction transaction = new Transaction.TransactionBuilder(command.getTimestamp(),
                    "Insufficient funds", senderAccount.getIban())
                    .build();
            senderUser.addTransaction(transaction);
            throw new InsufficientFundsException("Insufficient funds.");
        } catch (AccountNotFoundException | UserNotFoundException e) {
            return;
        }
    }

    private void makeTransaction(final Account senderAccount, final Account receiverAccount,
                                 final User senderUser, final User receiverUser,
                                 final String senderIBAN, final String receiverIBAN,
                                 final double amount, final CommandInput command,
                                 final CommandContext context)
        throws InsufficientFundsException {
        System.out.println(command.getCommand() + " " + command.getTimestamp());
        // Verify if the sender has enough funds
        if (senderAccount.getBalance() < amount) {
            throw new InsufficientFundsException("Insufficient funds.");
        }

        // Verify if the sender and receiver have the same currency
        String senderCurrency = senderAccount.getCurrency();
        String receiverCurrency = receiverAccount.getCurrency();

        double convertedAmount;
        try {
            convertedAmount = context.getCurrencyConverter().convertCurrency(amount,
                    senderCurrency, receiverCurrency);
            System.out.println("amount: " + amount + " " + senderCurrency);
            System.out.println("convertedAmount: " + convertedAmount + " " + receiverCurrency);
        } catch (CurrencyConversionException e) {
            addError(context.getOutput(), e.getMessage(),
                    command.getTimestamp(), "sendMoney");
            return;
        }

        // Add commission
        double commission = senderUser.getCurrentPlan().calculateTransactionFee(amount);
        System.out.println("commission: " + commission);
        double finalAmount = amount + commission;

        // Make the actual transaction
        senderAccount.setBalance(senderAccount.getBalance() - finalAmount);
        receiverAccount.setBalance(receiverAccount.getBalance() + convertedAmount);
        System.out.println("senderAccount balance: " + senderAccount.getBalance());
        System.out.println("receiverAccount balance: " + receiverAccount.getBalance());

        // Add the transaction to the sender's account
        Transaction senderTransaction = new Transaction.TransactionBuilder(command.getTimestamp(),
                command.getDescription(), senderAccount.getIban())
                .senderIBAN(senderIBAN)
                .receiverIBAN(receiverIBAN)
                .amountCurrency(amount + " " + senderCurrency)
                .transferType("sent")
                .build();
        senderUser.addTransaction(senderTransaction);

        // Add the transaction to the receiver's account
        Transaction receiverTransaction = new Transaction.TransactionBuilder(command.getTimestamp(),
                command.getDescription(),
                receiverAccount.getIban())
                .senderIBAN(senderIBAN)
                .receiverIBAN(receiverIBAN)
                .amountCurrency(convertedAmount + " " + receiverCurrency)
                .transferType("received")
                .build();
        receiverUser.addTransaction(receiverTransaction);
    }

    /**
     * Make an alias for a specific account.
     * @param command   Command input containing email, card number, amount, and currency.
     */
    public void makeAnAlias(final CommandInput command,
                                   final CommandContext context) {
        String email = command.getEmail();
        String alias = command.getAlias();

        try {
            User user = findUserByEmail(context.getUsers(), email);
            if (user == null) {
                throw new UserNotFoundException("User not found");
            }
            Account account = user.findAccountByIban(command.getAccount());
            if (account == null) {
                throw new AccountNotFoundException("Account not found");
            }
            // Check if the alias is already in use
            if (user.hasAlias(alias)) {
                throw new IllegalArgumentException("Alias already in use");
            }
            // Set the alias for the account
            account.setAlias(alias);
        } catch (UserNotFoundException | AccountNotFoundException e) {
                addError(context.getOutput(), e.getMessage(),
                        command.getTimestamp(), "makeAnAlias");
        } catch (IllegalArgumentException e) {
            return;
        }
    }

    /**
     * Print the transactions of a specific user.
     * @param command   Command input containing email, card number, amount, and currency.
     */
    public void history(final CommandInput command,
                               final CommandContext context) {
        User user = findUserByEmail(context.getUsers(), command.getEmail());
        try {
            if (user == null) {
                throw new UserNotFoundException("User not found");
            }
        } catch (UserNotFoundException e) {
            addError(context.getOutput(), e.getMessage(),
                    command.getTimestamp(), "history");
            return;
        }

        ArrayNode transactionsArray = context.getObjectMapper().createArrayNode();
        user.printTransactions(transactionsArray, context.getOutput());

        ObjectNode commandNode = context.getObjectMapper().createObjectNode();
        commandNode.put("command", "printTransactions");
        commandNode.set("output", transactionsArray);
        commandNode.put("timestamp", command.getTimestamp());
        context.getOutput().add(commandNode);
    }

    /**
     * Set the minimum balance for a specific account.
     * @param command   Command input containing email, card number, amount, and currency.
     */
    public void setMinBalance(final CommandInput command,
                              final CommandContext context)
            throws AccountNotFoundException {
        try {
            // Search for the user's account by Iban
            Account account = User.findAccountByIBAN(context.getUsers(), command.getAccount());
            if (account == null) {
                throw new AccountNotFoundException("Account not found");
            }
            // Set the minimum balance
            account.setMinimumBalance(command.getAmount());
        } catch (AccountNotFoundException e) {
            addError(context.getOutput(), e.getMessage(),
                    command.getTimestamp(), "setMinimumBalance");
        }
    }

    /**
     * Check the status of a specific card.
     * Add an error to the output if the card is not found.
     * Add a transaction to the user if the balance is less than the minimum balance.
     * @param command   Command input containing email, card number, amount, and currency.
     */
    public void checkCardStatus(final CommandInput command,
                                final CommandContext context)
            throws CardNotFoundException, InsufficientFundsException {
        // Search for the card in the users' accounts
        Card card = null;
        Account cardAccount = null;
        User cardUser = null;

        for (User user : context.getUsers()) {
            for (Account account : user.getAccounts()) {
                card = account.findCardByNumber(command.getCardNumber());
                if (card != null) {
                    cardUser = user;
                    cardAccount = account;
                    break;
                }
            }
            if (card != null) {
                break;
            }
        }

        try {
            if (cardUser == null) {
                throw new CardNotFoundException("Card not found");
            }
            double balance = cardAccount.getBalance();
            double minBalance = cardAccount.getMinimumBalance();

            // Checking the conditions for "frozen" and "warning"
            if (balance <= minBalance) {
                throw new InsufficientFundsException("You have reached the minimum "
                        + "amount of funds, the card will be frozen");
            }
        } catch (CardNotFoundException e) {
            addError(context.getOutput(), e.getMessage(),
                    command.getTimestamp(), "checkCardStatus");
        } catch (InsufficientFundsException e) {
            Transaction transaction = new Transaction.TransactionBuilder(command.getTimestamp(),
                    e.getMessage(),
                    cardAccount.getIban())
                    .build();
            cardUser.addTransaction(transaction);
        }
    }

    /**
     * Split a payment between multiple users.
     * Add an error to the output if the account is not found
     * or if the currency conversion is not supported.
     * @param command   Command input containing email, card number, amount, and currency.
     */
    public void splitPayment(final CommandInput command,
                                    final CommandContext context) {
        List<String> accountsForSplit =  new ArrayList<>();
        accountsForSplit = command.getAccounts();

        double totalAmount = command.getAmount();
        String currency = command.getCurrency();
        int timestamp = command.getTimestamp();

        // Calculate the amount per account
        double amountPerAccount = totalAmount / accountsForSplit.size();

        // Check if the accounts exist and have enough funds
        List<Account> validAccounts = new ArrayList<>();
        List<Account> invalidAccounts = new ArrayList<>();
        String lastInvalidIban = null;

        for (String iban : accountsForSplit) {
            Account account = User.findAccountByIBAN(context.getUsers(), iban);

            if (account == null) {
                addError(context.getOutput(), "Account not found: " + iban,
                        timestamp, "splitPayment");
                return;
            }

            try {
                // Convert the amount to the account currency
                double convertedAmount;
                convertedAmount = context.getCurrencyConverter().convertCurrency(amountPerAccount,
                                                                currency, account.getCurrency());
                // Check if the account has enough funds
                if (account.getBalance() - convertedAmount <= account.getMinimumBalance()) {
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

        if (!invalidAccounts.isEmpty()) {
            for (String involvedIban : accountsForSplit) {
                Account involvedAccount = User.findAccountByIBAN(context.getUsers(), involvedIban);
                if (involvedAccount != null) {
                    Transaction errorTransaction = new Transaction.TransactionBuilder(timestamp,
                            "Split payment of " + String.format("%.2f", totalAmount)
                                    + " " + currency,
                            involvedAccount.getIban())
                            .amount(amountPerAccount)
                            .amountCurrency(currency)
                            .involvedAccounts(accountsForSplit)
                            .error("Account " + lastInvalidIban
                                    + " has insufficient funds for a split payment.")
                            .build();
                    User involvedUser = User.findUserByAccount(context.getUsers(), involvedAccount);
                    if (involvedUser == null) {
                        return;
                    }
                    involvedUser.addTransaction(errorTransaction);
                }
            }
        }

        if (validAccounts.size() != accountsForSplit.size()) {
            return;
        }

        // Make the payment for each account if all accounts have enough funds
        for (Account account : validAccounts) {
            User user = User.findUserByAccount(context.getUsers(), account);
            if (user == null) {
                return;
            }
            double convertedAmount;
            try {
                convertedAmount = context.getCurrencyConverter().convertCurrency(amountPerAccount,
                        currency, account.getCurrency());
            } catch (CurrencyConversionException e) {
                addError(context.getOutput(),
                        "Currency conversion not supported for account: " + account.getIban(),
                        timestamp, "splitPayment");
                return;
            }

            account.setBalance(account.getBalance() - convertedAmount);
            // Add a transaction to the user
            Transaction successTransaction = new Transaction.TransactionBuilder(timestamp,
                    "Split payment of " + String.format("%.2f", totalAmount) + " " + currency,
                    account.getIban())
                    .error(null)
                    .amount(amountPerAccount)
                    .amountCurrency(currency)
                    .involvedAccounts(accountsForSplit)
                    .build();
            user.addTransaction(successTransaction);
        }
    }

    /**
     * Generate a report with the transactions of a specific account.
     * @param command   Command input containing email, card number, amount, and currency.
     */
    public void report(final CommandInput command,
                              final CommandContext context) {
        Account account = User.findAccountByIBAN(context.getUsers(), command.getAccount());
        if (account == null) {
            addError(context.getOutput(), "Account not found",
                    command.getTimestamp(), command.getCommand());
            return;
        }

        User user = User.findUserByAccount(context.getUsers(), account);
        if (user == null) {
            addError(context.getOutput(), "User not found",
                    command.getTimestamp(), command.getCommand());
            return;
        }

        // Create the principal node for the output
        ObjectNode reportNode = context.getObjectMapper().createObjectNode();
        reportNode.put("IBAN", account.getIban());
        reportNode.put("balance", account.getBalance());
        reportNode.put("currency", account.getCurrency());

        // Select the transactions in the specified time interval
        ArrayNode transactionsArray = context.getObjectMapper().createArrayNode();
        user.printReportTransactions(transactionsArray, context.getOutput(),
                command.getStartTimestamp(), command.getEndTimestamp(), account.getIban());
        reportNode.set("transactions", transactionsArray);

        // Add the final output to the output array
        ObjectNode commandNode = context.getObjectMapper().createObjectNode();
        commandNode.put("command", "report");
        commandNode.set("output", reportNode);
        commandNode.put("timestamp", command.getTimestamp());
        context.getOutput().add(commandNode);
    }

    /**
     * Generate a report with the spendings of a saving account.
     * @param command   Command input containing email, card number, amount, and currency.
     */
    public void spendingsReport(final CommandInput command,
                                       final CommandContext context) {
        Account account = User.findAccountByIBAN(context.getUsers(), command.getAccount());
        if (account == null) {
            addError(context.getOutput(), "Account not found",
                    command.getTimestamp(), command.getCommand());
            return;
        }

        if (account.getType().equals("savings")) {
            addErrorType(context.getOutput(),
                    "This kind of report is not supported for a saving account",
                    command.getTimestamp(), command.getCommand());
            return;
        }

        User user = User.findUserByAccount(context.getUsers(), account);
        if (user == null) {
            addError(context.getOutput(), "User not found",
                    command.getTimestamp(), command.getCommand());
            return;
        }

        // Create the principal node for the output
        ObjectNode reportNode = context.getObjectMapper().createObjectNode();
        reportNode.put("IBAN", account.getIban());
        reportNode.put("balance", account.getBalance());
        reportNode.put("currency", account.getCurrency());

        // Filter the transactions and add them to the transactions node
        ArrayNode transactionsArray = context.getObjectMapper().createArrayNode();
        Map<String, Double> spendingsByCommerciant = new HashMap<>();

        user.filterTransactionsByTypeAndInterval(transactionsArray,
                spendingsByCommerciant, command.getStartTimestamp(),
                command.getEndTimestamp(), context.getObjectMapper(), account.getIban());

        // Add the spendings by commerciant to the report
        reportNode.set("transactions", transactionsArray);

        // Create the list of commerciants with total spendings
        ArrayNode commerciantsArray = context.getObjectMapper().createArrayNode();

        // Sort the commerciants by name
        List<Map.Entry<String, Double>> sortedCommerciants;
        sortedCommerciants = new ArrayList<>(spendingsByCommerciant.entrySet());
        sortedCommerciants.sort(Map.Entry.comparingByKey());

        // Add the sort commerciants in the Json node
        for (Map.Entry<String, Double> entry : sortedCommerciants) {
            ObjectNode commerciantNode = context.getObjectMapper().createObjectNode();
            commerciantNode.put("commerciant", entry.getKey());
            commerciantNode.put("total", entry.getValue());
            commerciantsArray.add(commerciantNode);
        }

        // Add the commerciants to the report
        reportNode.set("commerciants", commerciantsArray);

        // Add the report to the output
        ObjectNode commandNode = context.getObjectMapper().createObjectNode();
        commandNode.put("command", "spendingsReport");
        commandNode.set("output", reportNode);
        commandNode.put("timestamp", command.getTimestamp());
        context.getOutput().add(commandNode);
    }

    /**
     * Add an error type to the output array.
     * @param output the output array
     * @param errorMessage the error message
     * @param timestamp the timestamp of the command
     * @param commandName the name of the command
     */
    public void addErrorType(final ArrayNode output,
                                    final String errorMessage,
                                    final int timestamp,
                                    final String commandName) {
        ObjectNode errorNode = output.addObject();
        errorNode.put("command", commandName);
        ObjectNode descriptionNode = errorNode.putObject("output");
        descriptionNode.put("error", errorMessage);
        errorNode.put("timestamp", timestamp);
    }

    /**
     * Collecting interest on savings accounts.
     * The interest rate is applied to the current balance.
     * If the account is not a savings account, an error is added to the output.
     * @param command the command to be executed
     * @param context the context of the command
     */
    public void addInterest(final CommandInput command,
                                   final CommandContext context) {
        try {
            Account account = User.findAccountByIBAN(context.getUsers(), command.getAccount());
            if (account == null) {
                throw new AccountNotFoundException("Account not found");
            }

            if (account.getType().equals("savings")) {
                account.addFunds(account.getBalance() * account.getInterestRate());
                return;
            }
            throw new IllegalArgumentException("This is not a savings account");
        } catch (AccountNotFoundException | IllegalArgumentException e) {
            CommandActions.addError(context.getOutput(), e.getMessage(),
                    command.getTimestamp(), command.getCommand());
        }
    }

    /**
     * Change the interest rate of a savings account.
     * If the account is not a savings account, an error is added to the output.
     * @param command the command to be executed
     * @param context the context of the command
     */
    public void changeInterestRate(final CommandInput command,
                                          final CommandContext context) {
        try {
            Account account = User.findAccountByIBAN(context.getUsers(), command.getAccount());
            if (account == null) {
                throw new AccountNotFoundException("Account not found");
            }
            if (account.getType().equals("savings")) {
                account.setInterestRate(command.getInterestRate());
            } else {
                throw new IllegalArgumentException("This is not a savings account");
            }

            User user = User.findUserByAccount(context.getUsers(), account);
            if (user == null) {
                throw new UserNotFoundException("User not found");
            }

            Transaction transaction = new Transaction.TransactionBuilder(command.getTimestamp(),
                    "Interest rate of the account changed to " + command.getInterestRate(),
                    account.getIban())
                    .build();
            user.addTransaction(transaction);

        } catch (AccountNotFoundException | IllegalArgumentException | UserNotFoundException e) {
            CommandActions.addError(context.getOutput(), e.getMessage(),
                    command.getTimestamp(), command.getCommand());
        }
    }

    public void withdrawSavings(final CommandInput command, final CommandContext context) {
        String accountIBAN = command.getAccount();
        double amount = command.getAmount();
        String currency = command.getCurrency();

        Account account = User.findAccountByIBAN(context.getUsers(), command.getAccount());
        if (account == null) {
            addError(context.getOutput(), "Account not found",
                    command.getTimestamp(), command.getCommand());
            return;
        }

        User user = User.findUserByAccount(context.getUsers(), account);
        if (user == null) {
            addError(context.getOutput(), "User not found",
                    command.getTimestamp(), command.getCommand());
            return;
        }

        if (!account.getType().equals("savings")) {
            Transaction transaction = new Transaction.TransactionBuilder(command.getTimestamp(),
                    "Account is not of type savings.", accountIBAN)
                    .build();
            user.addTransaction(transaction);
            return;
        }

        // Check if the user is of minimum age
        if (!user.isOfMinimumAge(21)) {
            Transaction transaction = new Transaction.TransactionBuilder(command.getTimestamp(),
                    "You don't have the minimum age required.", accountIBAN)
                    .build();
            user.addTransaction(transaction);
            return;
        }

        // Check if the account has enough funds
        if (account.getBalance() < amount) {
            Transaction transaction = new Transaction.TransactionBuilder(command.getTimestamp(),
                    "Insufficient funds", accountIBAN)
                    .build();
            user.addTransaction(transaction);
            return;
        }

        // Search if the user has a classic account with the same currency
        Account classicAccount = user.getAccounts().stream()
                .filter(acc -> acc.getType().equals("classic") && acc.getCurrency().equals(currency))
                .findFirst().orElse(null);

        if (classicAccount == null) {
            Transaction transaction = new Transaction.TransactionBuilder(command.getTimestamp(),
                    "You do not have a classic account.", accountIBAN)
                    .build();
            user.addTransaction(transaction);
            return;
        }

        double convertedAmount;
        try {
            convertedAmount = context.getCurrencyConverter().convertCurrency(amount,
                    command.getCurrency(), classicAccount.getCurrency());
        } catch (CurrencyConversionException e) {
            addError(context.getOutput(), e.getMessage(),
                    command.getTimestamp(), "withdrawSavings");
            return;
        }

        account.setBalance(account.getBalance() - amount);
        classicAccount.setBalance(classicAccount.getBalance() + convertedAmount);

        Transaction transaction = new Transaction.TransactionBuilder(command.getTimestamp(),
                "Savings withdrawal", accountIBAN)
                .build();
        user.addTransaction(transaction);
    }

    /**
     * Upgrade the plan of a user. The user can upgrade from a standard or student plan to a silver and
     * @param command the command to be executed
     * @param context the context of the command
     */
    public void upgradePlan(final CommandInput command, final CommandContext context) {
        String accountIBAN = command.getAccount();
        String newPlanType = command.getNewPlanType();

        Account account = User.findAccountByIBAN(context.getUsers(), command.getAccount());
        if (account == null) {
            addError(context.getOutput(), "Account not found",
                    command.getTimestamp(), command.getCommand());
            return;
        }

        User user = User.findUserByAccount(context.getUsers(), account);
        if (user == null) {
            addError(context.getOutput(), "User not found",
                    command.getTimestamp(), command.getCommand());
            return;
        }

        System.out.println("accountIBAN: " + accountIBAN + "vrea sa treaca de la planul "
                + user.getCurrentPlan().getPlanType() + " la planul " + newPlanType);

        // Check if the user already has the desired plan
        if (user.getCurrentPlan().getPlanType().equals(newPlanType)) {
            Transaction transaction = new Transaction.TransactionBuilder(command.getTimestamp(),
                    "The user already has the " + newPlanType + " plan.", accountIBAN)
                    .currentPlan(user.getCurrentPlan().getPlanType())
                    .build();
            user.addTransaction(transaction);
            return;
        }

        // Check if the user can upgrade to the desired plan
        List<String> planHierarchy = List.of("standard", "student", "silver", "gold");
        int currentPlanIndex = planHierarchy.indexOf(user.getCurrentPlan().getPlanType());
        int newPlanIndex = planHierarchy.indexOf(newPlanType);

        if (newPlanIndex < currentPlanIndex) {
            Transaction transaction = new Transaction.TransactionBuilder(command.getTimestamp(),
                    "You cannot downgrade your plan.", accountIBAN)
                    .currentPlan(user.getCurrentPlan().getPlanType())
                    .build();
            user.addTransaction(transaction);
            return;
        }

        int approvedTransactions = user.countCardPaymentTransaction(command.getAccount(), 300, context);
        // If the current plan is silver and the user has at least 5 approved transactions
        // of at least 300 RON, the upgrade is made automatically to gold
        if (currentPlanIndex == 2 && approvedTransactions >= 5) {
            user.setCurrentPlan(new GoldPlan());
            System.out.println("Userul a trecut automat la Gold");
            Transaction transaction = new Transaction.TransactionBuilder(command.getTimestamp(),
                    "Upgrade plan", accountIBAN)
                    .currentPlan(user.getCurrentPlan().getPlanType())
                    .build();
            user.addTransaction(transaction);
            return;
        }

        // Calculate the fee for the upgrade (in RON)
        double fee = 0;
        if (currentPlanIndex == 0 && newPlanIndex == 2) fee = 100; // standard to silver
        if (currentPlanIndex == 1 && newPlanIndex == 2) fee = 100; // student to silver
        if (currentPlanIndex == 2 && newPlanIndex == 3) fee = 250; // silver to gold
        if (currentPlanIndex == 0 && newPlanIndex == 3) fee = 350; // standard to gold
        if (currentPlanIndex == 1 && newPlanIndex == 3) fee = 350; // student to golf

        double convertedFee;
        try {
            convertedFee = context.getCurrencyConverter().convertCurrency(fee,
                    "RON", account.getCurrency());
            // Round the converted fee to two decimals
            convertedFee = roundToTwoDecimals(convertedFee);
        } catch (CurrencyConversionException e) {
            addError(context.getOutput(), e.getMessage(),
                    command.getTimestamp(), "upgradePlan");
            return;
        }
        System.out.println("convertedFee: " + convertedFee);

        if (account.getBalance() < convertedFee) {
            Transaction transaction = new Transaction.TransactionBuilder(command.getTimestamp(),
                    "Insufficient funds", accountIBAN)
                    .build();
            user.addTransaction(transaction);
            return;
        }
        // Debit the fee from the account
        account.setBalance(account.getBalance() - convertedFee);
        System.out.println("account balance: " + account.getBalance());
        ServicePlan newPlan = switch (newPlanType) {
            case "silver" -> new SilverPlan();
            case "gold" -> new GoldPlan();
            default -> throw new IllegalArgumentException("Invalid plan type");
        };
        user.setCurrentPlan(newPlan);

        Transaction transaction = new Transaction.TransactionBuilder(command.getTimestamp(),
                "Upgrade plan", accountIBAN)
                .currentPlan(user.getCurrentPlan().getPlanType())
                .build();
        user.addTransaction(transaction);
    }

    /**
     * Round a double value to two decimals.
     * @param value the value to be rounded
     * @return the rounded value
     */
    private double roundToTwoDecimals(double value) {
        return BigDecimal.valueOf(value)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    public void cashWithdrawal(final CommandInput command, final CommandContext context) throws UserNotFoundException,
            CardNotFoundException,
            UnauthorizedCardAccessException,
            InsufficientFundsException,
            UnauthorizedCardStatusException {

        String email = command.getEmail();
        String cardNumber = command.getCardNumber();
        int timestamp = command.getTimestamp();
        double amount = command.getAmount();

        User user = findUserByEmail(context.getUsers(), email);
        Card cardUser = null;
        Account accountUser = null;

        try {
            // Check if the user exists
            if (user == null) {
                throw new UserNotFoundException("User not found");
            }

            // Search for the card in each account of the user
            for (Account account : user.getAccounts()) {
                if (account.findCardByNumber(cardNumber) != null) {
                    cardUser = account.findCardByNumber(cardNumber);
                    accountUser = account;
                    break;
                }
            }

            // If the card was not found, add an error to the output
            if (cardUser == null) {
                throw new CardNotFoundException("Card not found");
            }

            // Check if the user owns the card
            if (!accountUser.getOwner().equals(email)) {
                throw new UnauthorizedCardAccessException("User does not own the card");
            }

            // Calculate commission
            double commission = user.getCurrentPlan().calculateTransactionFee(amount);
            System.out.println("commission: " + commission);

            // Apply commission and cashback
            double finalAmount = amount + commission;
            System.out.println("finalAmount: " + finalAmount);

            // Check if the card is active amd if the account has enough funds
            double newBalance = accountUser.getBalance() - finalAmount;
            if (newBalance < accountUser.getMinimumBalance()) {
                if (cardUser.getStatus().equals("active")
                        && finalAmount > accountUser.getBalance()) {
                    throw new InsufficientFundsException("Insufficient funds");
                }
            }

            // Make the payment
            accountUser.setBalance(accountUser.getBalance() - finalAmount);
            if (cardUser.getStatus().equals("active")
                    && accountUser.getBalance() >= accountUser.getMinimumBalance()) {
                cardUser.setStatus("active");
                Transaction transaction = new Transaction.TransactionBuilder(timestamp,
                        "Cash withdrawal of " + amount, accountUser.getIban())
                        .amount(amount)
                        .error("Cash withdrawal")
                        .build();
                user.addTransaction(transaction);
            } else {
                // The payment cannot be made, so the amount is returned to the account
                // and the card is frozen
                cardUser.setStatus("frozen");
                accountUser.setBalance(accountUser.getBalance() + finalAmount);
                throw new UnauthorizedCardStatusException("The card is frozen");
            }
        } catch (UserNotFoundException | CardNotFoundException | UnauthorizedCardAccessException e) {
            addError(context.getOutput(), e.getMessage(), timestamp, "cashWithdrawal");
        } catch (InsufficientFundsException | UnauthorizedCardStatusException e) {
            // Add a transaction to the account
            Transaction transaction = new Transaction.TransactionBuilder(timestamp,
                    e.getMessage(), accountUser.getIban()).build();
            user.addTransaction(transaction);
        }
    }

    /**
     * Handle the default case when the command is not recognized.
     * @param command the command to be executed
     * @param context the context of the command
     */
    public void handleDefault(final CommandInput command, final CommandContext context) {
        System.out.println("Invalid command: " + command.getCommand());
    }
}
