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

import org.poo.models.Account;
import org.poo.models.Card;
import org.poo.models.Transaction;
import org.poo.models.User;
import org.poo.models.SplitPayment;
import org.poo.models.PaymentProcessor;
import org.poo.services.Commerciant;
import org.poo.services.CashbackStrategy;
import org.poo.services.GoldPlan;

import org.poo.fileio.CommandInput;
import org.poo.services.*;
import org.poo.utils.Utils;

import java.util.*;

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
                accountNode.put("balance", account.getBalance());
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
        System.out.println("addAccount " + command.getTimestamp() + " " + command.getAccountType());
        User user = findUserByEmail(context.getUsers(), command.getEmail());
        try {
            if (user == null) {
                throw new UserNotFoundException("User not found");
            }

            String iban = Utils.generateIBAN();
            user.addAccount(new Account(iban, command.getCurrency(),
                    command.getAccountType(), command.getEmail()));

            if (command.getAccountType().equals("savings")) {
                user.findAccountByIban(iban).setInterestRate(command.getInterestRate());
            }

            if (command.getAccountType().equals("business")) {
                user.setRole("owner");
            }

            Transaction transaction = new Transaction.TransactionBuilder(command.getTimestamp(),
                    "New account created", iban, "create")
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
                        "New card created", account.getIban(), "create")
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
                    "New card created", account.getIban(), "create")
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
        System.out.println("addFunds " + command.getTimestamp());
        User emailUser = findUserByEmail(context.getUsers(), command.getEmail());
        if (emailUser == null) {
            addError(context.getOutput(), "User not found", command.getTimestamp(), "addFunds");
            return;
        }
        Account userAccount = emailUser.findAccountByIban(command.getAccount());

        // Add funds to the user's account
        if (userAccount != null) {
            userAccount.addFunds(command.getAmount());
            System.out.println("S-au adaugat: " + command.getAmount());
            System.out.println("account balance " + userAccount.getIban() + " adaugarea de fonduri: " + userAccount.getBalance() + " " + userAccount.getCurrency());
            Transaction transaction = new Transaction.TransactionBuilder(command.getTimestamp(),
                    "Funds added", userAccount.getIban(), "deposit")
                    .amount(command.getAmount())
                    .build();
            emailUser.addTransaction(transaction);
            return;
        }

        System.out.println("emailUser: " + emailUser.getEmail() + " " + emailUser.getRole());
        for (User user : context.getUsers()) {
            Account account = user.findAccountByIban(command.getAccount());
            if (account != null && !emailUser.getRole().equals("employee")) {
                account.addFunds(command.getAmount());
                System.out.println("S-au adaugat: " + command.getAmount());
                System.out.println("account balance " + account.getIban() + " adaugarea de fonduri: " + account.getBalance() + " " + account.getCurrency());
                Transaction transaction = new Transaction.TransactionBuilder(command.getTimestamp(),
                        "Funds added", account.getIban(), "deposit")
                        .amount(command.getAmount())
                        .build();
                emailUser.addTransaction(transaction);
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
                        accountToDelete.getIban(), "delete")
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
                        "The card has been destroyed", account.getIban(), "delete")
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
    public void payOnline(final CommandInput command,
                          final CommandContext context) throws UserNotFoundException,
                                                               CardNotFoundException,
                                                               UnauthorizedCardAccessException,
                                                               InsufficientFundsException,
                                                               UnauthorizedCardStatusException {
        System.out.println("payOnline " + command.getTimestamp());

        if (command.getAmount() <= 0) {
            System.out.println("Invalid amount");
            return;
        }
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
            System.out.println("Userul este: " + user.getRole());

            Account ownerAccount = user.getOwnerAccount();

            // Search for the card in each account of the user
            for (Account account : user.getAccounts()) {
                if (account.findCardByNumber(cardNumber) != null) {
                    cardUser = account.findCardByNumber(cardNumber);
                    accountUser = account;
                    break;
                }
            }

            if (cardUser == null && (!user.getRole().equals("owner") && !user.getRole().equals("user"))) {
                System.out.println("Caut printe cardurile ownerului");
                if (ownerAccount.findCardByNumber(cardNumber) != null) {
                    System.out.println("Cardul a fost gasit printre cardurile ownerului");
                    cardUser = ownerAccount.findCardByNumber(cardNumber);
                    accountUser = ownerAccount;
                }
            }

            // If the card was not found, add an error to the output
            if (cardUser == null) {
                System.out.println("Card not found");
                throw new CardNotFoundException("Card not found");
            }

            double amountInRON = context.getCurrencyConverter().
                    convertCurrency(command.getAmount(), command.getCurrency(), "RON");
            System.out.println("amountInRON: " + amountInRON + " RON");

            if (user.getRole().equals("employee") && amountInRON > ownerAccount.getSpendingLimit()) {
                System.out.println("userul este employee si nu are dreptul sa cheltuie atatia bani");
                return;
            }

            // Check if the user owns the card
            if (!accountUser.getOwner().equals(email) && user.getRole().equals("owner")) {
                throw new UnauthorizedCardAccessException("User does not own the card");
            }

            // Convert the amount to the account currency
            double amountInAccountCurrency;
            amountInAccountCurrency = context.getCurrencyConverter().
                    convertCurrency(command.getAmount(),
                    command.getCurrency(),
                    accountUser.getCurrency());
            System.out.println("amountInAccountCurrency: " + amountInAccountCurrency + " " + accountUser.getCurrency());


            // Calculate commission in RON for the silver plan
            double commissionInRON = user.getCurrentPlan().calculateTransactionFee(amountInRON);

            double commission = context.getCurrencyConverter().
                    convertCurrency(commissionInRON,
                            "RON",
                            accountUser.getCurrency());
            System.out.println("commission: " + commission);

            // Calculate cashback
            Commerciant commerciant = context.findCommerciantByName(command.getCommerciant());
            System.out.println("commerciant: " + commerciant.getName() + " " + commerciant.getType());

            CashbackStrategy cashbackStrategy = commerciant.getCashbackStrategyInstance();
            String accountCurrency = accountUser.getCurrency();
            double cashback = cashbackStrategy.calculateCashback(user, commerciant, accountCurrency, amountInAccountCurrency, context);
            System.out.println("cashback: " + cashback);

            // Apply commission and cashback
            double finalAmount = amountInAccountCurrency + commission - cashback;
            System.out.println("finalAmount: " + finalAmount + " " + accountUser.getCurrency());

            // Check if the card is active amd if the account has enough funds
            double newBalance = accountUser.getBalance() - finalAmount;
            if (newBalance < accountUser.getMinimumBalance()) {
                if (cardUser.getStatus().equals("active")
                        && finalAmount > accountUser.getBalance()) {
                    System.out.println("Insufficient funds at timestamp " + command.getTimestamp());
                    throw new InsufficientFundsException("Insufficient funds");
                }
            }

            // Make the payment
            accountUser.setBalance(accountUser.getBalance() - finalAmount);
            accountUser.setBalance((accountUser.getBalance()));
            if (cardUser.getStatus().equals("active")
                    && accountUser.getBalance() >= accountUser.getMinimumBalance()) {
                cardUser.setStatus("active");
                System.out.println("S-A EFECTUAT TRANZACTIA CU SUCCES account balance " + accountUser.getIban() + " plata online: " + accountUser.getBalance() + " " + accountUser.getCurrency());
                accountUser.setBalance((accountUser.getBalance()));

                Transaction transaction = new Transaction.TransactionBuilder(timestamp,
                        "Card payment", accountUser.getIban(), "spending")
                        .amount((amountInAccountCurrency))
                        .amountCurrency(accountUser.getCurrency())
                        .commerciant(command.getCommerciant())
                        .build();
                user.addTransaction(transaction);

                if (amountInRON > 300 && user.getCurrentPlan().getPlanType().equals("silver")) {
                    System.out.println("Userul are plan " + user.getCurrentPlan().getPlanType()
                            + " si a efectuat o plata mai mare de 300 RON");
                    user.setCountSilverPayments(user.getCountSilverPayments() + 1);
                }

                if (user.getCountSilverPayments() >= 5) {
                    System.out.println("Userul a efectuat 5 plati mai mari de 300 RON si i se va upgrada planul");
                    user.setCurrentPlan(new GoldPlan());
                    user.setCountSilverPayments(0);
                    Transaction upgradeTransaction = new Transaction.TransactionBuilder(command.getTimestamp(),
                            "Upgrade plan", accountUser.getIban(), "upgrade")
                            .currentPlan(user.getCurrentPlan().getPlanType())
                            .build();
                    user.addTransaction(upgradeTransaction);
                }
                // If the card is the type of "one time pay",
                // it is destroyed after the payment and a new card is created
                if (cardUser.getType().equals("one time pay")) {
                    cardUser.setStatus("destroyed");
                    accountUser.getCards().remove(cardUser);
                    Transaction transaction1 = new Transaction.TransactionBuilder(timestamp,
                            "The card has been destroyed", accountUser.getIban(), "delete")
                            .card(cardNumber)
                            .cardHolder(user.getEmail())
                            .build();
                    user.addTransaction(transaction1);

                    String newCardNumber = Utils.generateCardNumber();
                    Card oneTimeCard = new Card(newCardNumber, "active", "one time pay");
                    accountUser.addCard(oneTimeCard);

                    Transaction transaction2;
                    transaction2 = new Transaction.TransactionBuilder(command.getTimestamp(),
                            "New card created", accountUser.getIban(), "delete")
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
                    e.getMessage(), accountUser.getIban(), "error").build();
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

            System.out.println("sendMoney " + command.getTimestamp());
            if (senderAccount == null  && User.findAccountByAlias(context.getUsers(), senderIBAN) != null ) {
                senderAccount = User.findAccountByAlias(context.getUsers(), senderIBAN);
            }
            if (senderAccount == null) {
                throw new AccountNotFoundException("User not found");
            }

            if (receiverAccount == null && context.findCommerciantByIban(receiverIBAN) != null) {
                Commerciant receiverCommerciant = context.findCommerciantByIban(receiverIBAN);
                System.out.println("Comanda sendMoney de la timestamp: " + command.getTimestamp() + " functioneaza ca o plata online catre un comerciant");
                System.out.println("receiverCommerciant: " + receiverCommerciant.getName());
                sendMoneyCommerciant(command, context, receiverCommerciant);
                return;
            }

            if (receiverAccount == null) {
                throw new AccountNotFoundException("User not found");
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
                    "Insufficient funds", senderAccount.getIban(), "error")
                    .build();
            senderUser.addTransaction(transaction);
            throw new InsufficientFundsException("Insufficient funds.");
        } catch (AccountNotFoundException | UserNotFoundException | CurrencyConversionException e) {
            addError(context.getOutput(), e.getMessage(),
                    command.getTimestamp(), "sendMoney");
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
        System.out.println("Before transaction");
        System.out.println("senderAccount balance: " + senderAccount.getBalance() + " " + senderCurrency);
        System.out.println("receiverAccount balance: " + receiverAccount.getBalance() + " " + receiverCurrency);

        double convertedAmount;
        double amountInRON;
        try {
            convertedAmount = context.getCurrencyConverter().convertCurrency(amount,
                    senderCurrency, receiverCurrency);
            amountInRON = context.getCurrencyConverter().convertCurrency(amount, senderCurrency, "RON");
            System.out.println("amount: " + amount + " " + senderCurrency);
            System.out.println("convertedAmount: " + convertedAmount + " " + receiverCurrency);
        } catch (CurrencyConversionException e) {
            addError(context.getOutput(), e.getMessage(),
                    command.getTimestamp(), "sendMoney");
            return;
        }

        // Add commission
        double commissionInRON = senderUser.getCurrentPlan().calculateTransactionFee(amountInRON);
        double commission;
        try {
            commission = context.getCurrencyConverter().convertCurrency(commissionInRON,
                    "RON", senderCurrency);
        } catch (CurrencyConversionException e) {
            addError(context.getOutput(), e.getMessage(),
                    command.getTimestamp(), "sendMoney");
            return;
        }
        System.out.println("commission: " + commission);
        double finalAmount = amount + commission;

        double newBalance = senderAccount.getBalance() - finalAmount;
        if (newBalance < senderAccount.getMinimumBalance()) {
            System.out.println("Insufficient funds at timestamp " + command.getTimestamp());
            throw new InsufficientFundsException("Insufficient funds");
        }
        // Make the actual transaction
        senderAccount.setBalance((senderAccount.getBalance() - finalAmount));
        receiverAccount.setBalance((receiverAccount.getBalance() + convertedAmount));
        System.out.println("senderAccount balance: " + senderAccount.getBalance() + " " + senderCurrency);
        System.out.println("receiverAccount balance: " + receiverAccount.getBalance() + " " + receiverCurrency);

        // Add the transaction to the sender's account
        Transaction senderTransaction = new Transaction.TransactionBuilder(command.getTimestamp(),
                command.getDescription(), senderAccount.getIban(), "spending")
                .senderIBAN(senderIBAN)
                .receiverIBAN(receiverIBAN)
                .amountCurrency(amount + " " + senderCurrency)
                .transferType("sent")
                .build();
        senderUser.addTransaction(senderTransaction);

        // Add the transaction to the receiver's account
        Transaction receiverTransaction = new Transaction.TransactionBuilder(command.getTimestamp(),
                command.getDescription(),
                receiverAccount.getIban(),
                "deposit")
                .senderIBAN(senderIBAN)
                .receiverIBAN(receiverIBAN)
                .amountCurrency(convertedAmount + " " + receiverCurrency)
                .transferType("received")
                .build();
        receiverUser.addTransaction(receiverTransaction);

        if (amountInRON > 300 && senderUser.getCurrentPlan().getPlanType().equals("silver")) {
            System.out.println("Userul are plan " + senderUser.getCurrentPlan().getPlanType()
                    + " si a efectuat o plata mai mare de 300 RON");
            senderUser.setCountSilverPayments(senderUser.getCountSilverPayments() + 1);
        }

        if (senderUser.getCountSilverPayments() >= 5) {
            System.out.println("Userul a efectuat 5 plati mai mari de 300 RON si i se va upgrada planul");
            senderUser.setCurrentPlan(new GoldPlan());
            senderUser.setCountSilverPayments(0);
            Transaction upgradeTransaction = new Transaction.TransactionBuilder(command.getTimestamp(),
                    "Upgrade plan", senderAccount.getIban(), "upgrade")
                    .currentPlan(senderUser.getCurrentPlan().getPlanType())
                    .build();
            senderUser.addTransaction(upgradeTransaction);
        }
    }

    private void sendMoneyCommerciant(final CommandInput command,
                                      final CommandContext context,
                                      final Commerciant receiverCommerciant)
            throws UserNotFoundException, InsufficientFundsException, CurrencyConversionException {

        String senderIBAN = command.getAccount();
        double amount = command.getAmount();
        String senderEmail = command.getEmail();
        int timestamp = command.getTimestamp();

        User senderUser = findUserByEmail(context.getUsers(), senderEmail);
        Account senderAccount = User.findAccountByIBAN(context.getUsers(), senderIBAN);

        if (senderUser == null || senderAccount == null) {
            throw new UserNotFoundException("Sender not found.");
        }
        System.out.println("sendMoneyCommerciant " + command.getTimestamp());
        System.out.println("senderAccount balance: " + senderAccount.getBalance() + " " + senderAccount.getCurrency());

        double amountInRON = context.getCurrencyConverter()
                .convertCurrency(command.getAmount(), senderAccount.getCurrency(), "RON");
        System.out.println("amountInRON: " + amountInRON + " RON");

        double commissionInRON = senderUser.getCurrentPlan().calculateTransactionFee(amountInRON);
        double commission = context.getCurrencyConverter()
                .convertCurrency(commissionInRON, "RON", senderAccount.getCurrency());
        System.out.println("commission: " + commission);

        CashbackStrategy cashbackStrategy = receiverCommerciant.getCashbackStrategyInstance();
        double cashback = cashbackStrategy.calculateCashback(senderUser, receiverCommerciant,
                senderAccount.getCurrency(), amount, context);
        System.out.println("cashback: " + cashback);

        double finalAmount = amount + commission - cashback;
        System.out.println("finalAmount: " + finalAmount + " " + senderAccount.getCurrency());

        double newBalance = senderAccount.getBalance() - finalAmount;
        if (newBalance < senderAccount.getMinimumBalance()) {
            System.out.println("Insufficient funds for transaction at timestamp " + timestamp);
            throw new InsufficientFundsException("Insufficient funds.");
        }

        senderAccount.setBalance(newBalance);
        Transaction senderTransaction = new Transaction.TransactionBuilder(command.getTimestamp(),
                command.getDescription(), senderAccount.getIban(), "spending")
                .senderIBAN(senderIBAN)
                .receiverIBAN(receiverCommerciant.getAccount())
                .amountCurrency(amount + " " + senderAccount.getCurrency())
                .transferType("sent")
                .build();
        senderUser.addTransaction(senderTransaction);


        System.out.println("Tranzacția a fost efectuată cu succes către comerciant: "
                + receiverCommerciant.getName());
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
        System.out.println(command.getCommand() + " " + command.getTimestamp());
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
        System.out.println("Transactions for user " + user.getEmail());
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
                    cardAccount.getIban(),
                    "error")
                    .build();
            cardUser.addTransaction(transaction);
        }
    }

//    /**
//     * Split a payment between multiple users.
//     * Add an error to the output if the account is not found
//     * or if the currency conversion is not supported.
//     * @param command   Command input containing email, card number, amount, and currency.
//     */
//    public void splitPayment(final CommandInput command,
//                                    final CommandContext context) {
//        List<String> accountsForSplit =  new ArrayList<>();
//        accountsForSplit = command.getAccounts();
//
//        double totalAmount = command.getAmount();
//        String currency = command.getCurrency();
//        int timestamp = command.getTimestamp();
//
//        // Calculate the amount per account
//        double amountPerAccount = totalAmount / accountsForSplit.size();
//
//        // Check if the accounts exist and have enough funds
//        List<Account> validAccounts = new ArrayList<>();
//        List<Account> invalidAccounts = new ArrayList<>();
//        String lastInvalidIban = null;
//
//        for (String iban : accountsForSplit) {
//            Account account = User.findAccountByIBAN(context.getUsers(), iban);
//
//            if (account == null) {
//                addError(context.getOutput(), "Account not found: " + iban,
//                        timestamp, "splitPayment");
//                return;
//            }
//
//            try {
//                // Convert the amount to the account currency
//                double convertedAmount;
//                convertedAmount = context.getCurrencyConverter().convertCurrency(amountPerAccount,
//                                                                currency, account.getCurrency());
//                // Check if the account has enough funds
//                if (account.getBalance() - convertedAmount <= account.getMinimumBalance()) {
//                    lastInvalidIban = account.getIban();
//                    invalidAccounts.add(account);
//                } else {
//                    validAccounts.add(account);
//                }
//            } catch (CurrencyConversionException e) {
//                addError(context.getOutput(),
//                        "Currency conversion not supported for account: " + iban,
//                        timestamp, "splitPayment");
//                return;
//            }
//        }
//
//        if (!invalidAccounts.isEmpty()) {
//            for (String involvedIban : accountsForSplit) {
//                Account involvedAccount = User.findAccountByIBAN(context.getUsers(), involvedIban);
//                if (involvedAccount != null) {
//                    Transaction errorTransaction = new Transaction.TransactionBuilder(timestamp,
//                            "Split payment of " + String.format("%.2f", totalAmount)
//                                    + " " + currency,
//                            involvedAccount.getIban())
//                            .amount(amountPerAccount)
//                            .amountCurrency(currency)
//                            .involvedAccounts(accountsForSplit)
//                            .error("Account " + lastInvalidIban
//                                    + " has insufficient funds for a split payment.")
//                            .build();
//                    User involvedUser = User.findUserByAccount(context.getUsers(), involvedAccount);
//                    if (involvedUser == null) {
//                        return;
//                    }
//                    involvedUser.addTransaction(errorTransaction);
//                }
//            }
//        }
//
//        if (validAccounts.size() != accountsForSplit.size()) {
//            return;
//        }
//
//        // Make the payment for each account if all accounts have enough funds
//        for (Account account : validAccounts) {
//            User user = User.findUserByAccount(context.getUsers(), account);
//            if (user == null) {
//                return;
//            }
//            double convertedAmount;
//            try {
//                convertedAmount = context.getCurrencyConverter().convertCurrency(amountPerAccount,
//                        currency, account.getCurrency());
//            } catch (CurrencyConversionException e) {
//                addError(context.getOutput(),
//                        "Currency conversion not supported for account: " + account.getIban(),
//                        timestamp, "splitPayment");
//                return;
//            }
//
//            account.setBalance(account.getBalance() - convertedAmount);
//            // Add a transaction to the user
//            Transaction successTransaction = new Transaction.TransactionBuilder(timestamp,
//                    "Split payment of " + String.format("%.2f", totalAmount) + " " + currency,
//                    account.getIban())
//                    .error(null)
//                    .amount(amountPerAccount)
//                    .amountCurrency(currency)
//                    .involvedAccounts(accountsForSplit)
//                    .build();
//            user.addTransaction(successTransaction);
//        }
//    }

//    public void splitPayment(final CommandInput command, final CommandContext context) {
//        System.out.println("command: " + command.getCommand() + " " + command.getTimestamp());
//        String splitPaymentType = command.getSplitPaymentType();
//        List<String> accountsForSplit = command.getAccounts();
//        List<Double> amountsForUsers = command.getAmountForUsers();
//        String currency = command.getCurrency();
//        double totalAmount = command.getAmount();
//        int timestamp = command.getTimestamp();
//
//        // Check if the accounts and amounts are of the same size
//        if (accountsForSplit.size() != amountsForUsers.size()) {
//            addError(context.getOutput(), "Accounts and amounts size mismatch.", timestamp, "splitPayment");
//            return;
//        }
//
//        Map<String, Boolean> userResponses = new HashMap<>(); // Email -> Accept/Reject
//        List<Account> validAccounts = new ArrayList<>();
//        String lastInvalidIban = null;
//
//        // Check if the accounts are valid and have enough funds
//        for (int i = 0; i < accountsForSplit.size(); i++) {
//            String iban = accountsForSplit.get(i);
//            double amountForUser = amountsForUsers.get(i);
//
//            Account account = User.findAccountByIBAN(context.getUsers(), iban);
//            if (account == null) {
//                addError(context.getOutput(), "One of the accounts is invalid.", timestamp, "splitPayment");
//                return;
//            }
//
//            try {
//                // convert the amount to the account currency
//                double convertedAmount = context.getCurrencyConverter().convertCurrency(amountForUser, currency, account.getCurrency());
//                // check if the account has enough funds
//                if (account.getBalance() - convertedAmount < account.getMinimumBalance()) {
//                    lastInvalidIban = iban;
//                    addError(context.getOutput(),
//                            "Account " + lastInvalidIban + " has insufficient funds for a split payment.",
//                            timestamp, "splitPayment");
//                    return;
//                }
//            } catch (CurrencyConversionException e) {
//                addError(context.getOutput(),
//                        "Currency conversion not supported for account: " + iban,
//                        timestamp, "splitPayment");
//                return;
//            }
//
//            validAccounts.add(account);
//        }
//
//        // Initialize the pending split payment
//        context.getPendingSplitPayments().put(timestamp, userResponses);
//    }
//
//    // Procesare acceptare split payment
//    public void acceptSplitPayment(final CommandInput command, final CommandContext context) {
//        String email = command.getEmail();
//        int timestamp = command.getTimestamp();
//
//        for (Map.Entry<Integer, Map<String, Boolean>> entry : context.getPendingSplitPayments().entrySet()) {
//            Map<String, Boolean> responses = entry.getValue();
//            if (!responses.containsKey(email)) {
//                continue;
//            }
//
//            responses.put(email, true);
//
//            // Dacă toți au acceptat
//            if (responses.values().stream().allMatch(Boolean::booleanValue)) {
//                processSplitPayment(command, context);
//                context.getPendingSplitPayments().remove(entry.getKey());
//            }
//            return;
//        }
//    }
//
//    // Procesare refuz split payment
//    public void rejectSplitPayment(final CommandInput command, final CommandContext context) {
//        String email = command.getEmail();
//        int timestamp = command.getTimestamp();
//
//        for (Map.Entry<Integer, Map<String, Boolean>> entry : context.getPendingSplitPayments().entrySet()) {
//            Map<String, Boolean> responses = entry.getValue();
//            if (!responses.containsKey(email)) {
//                continue;
//            }
//
//            addError(context.getOutput(), "One user rejected the payment.", timestamp, "rejectSplitPayment");
//            context.getPendingSplitPayments().remove(entry.getKey());
//            return;
//        }
//    }
    public void splitPayment(final CommandInput command, final CommandContext context) {
        PaymentProcessor paymentProcessor = PaymentProcessor.getInstance();
        Map<String, String> emailToAccount = new HashMap<>();
        Map<String, Double> accountBalances = new HashMap<>();

        for (String iban : command.getAccounts()) {
            Account account = User.findAccountByIBAN(context.getUsers(), iban);
            if (account == null) {
                addError(context.getOutput(), "Account not found: " + iban,
                         command.getTimestamp(), "splitPayment");
                return;
            }
            emailToAccount.put(account.getOwner(), account.getIban());
            accountBalances.put(account.getIban(), account.getBalance());
        }

        SplitPayment splitPayment = new SplitPayment(command.getSplitPaymentType(),
                command.getAccounts(), command.getAmountForUsers(),
                command.getCurrency(), command.getAmount(),
                command.getTimestamp(), emailToAccount, accountBalances);
        paymentProcessor.addSplitPayment(splitPayment);
        paymentProcessor.printActiveCommands();
    }

    public void acceptSplitPayment(final CommandInput command, final CommandContext context) {
        System.out.println("acceptSplitPayment " + command.getTimestamp() + " for account " + command.getEmail());
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

    public void rejectSplitPayment(final CommandInput command, final CommandContext context) {
        System.out.println("rejectSplitPayment " + command.getTimestamp() + " for account " + command.getEmail());
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
        paymentProcessor.processResponse(command.getEmail(), false, context, command.getSplitPaymentType());
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

    public void businessReport(final CommandInput command, final CommandContext context) {
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
        double depositLimit = account.getDepositLimit();
        double spendingLimit = account.getSpendingLimit();
        try {
            depositLimit = context.getCurrencyConverter().convertCurrency(depositLimit, "RON", account.getCurrency());
            spendingLimit = context.getCurrencyConverter().convertCurrency(spendingLimit, "RON", account.getCurrency());
        } catch (CurrencyConversionException e) {
            addError(context.getOutput(), e.getMessage(),
                    command.getTimestamp(), command.getCommand());
            return;
        }

        // Creare nod pentru raport
        ObjectNode reportNode = context.getObjectMapper().createObjectNode();
        reportNode.put("IBAN", account.getIban());
        reportNode.put("balance", (account.getBalance()));
        reportNode.put("currency", account.getCurrency());
        reportNode.put("spending limit", (spendingLimit));
        reportNode.put("deposit limit", (depositLimit));
        reportNode.put("statistics type", command.getType());

        // Procesăm raportul în funcție de tip
        switch (command.getType()) {
            case "transaction":
                generateTransactionReport(reportNode, command, account, context);
                break;

            case "commerciant":
                generateCommerciantReport(reportNode, command, account, context);
                break;

            default:
                addError(context.getOutput(), "Invalid report type",
                        command.getTimestamp(), command.getCommand());
                return;
        }

        // Adăugăm raportul la context
        ObjectNode commandNode = context.getObjectMapper().createObjectNode();
        commandNode.put("command", "businessReport");
        commandNode.set("output", reportNode);
        commandNode.put("timestamp", command.getTimestamp());
        context.getOutput().add(commandNode);
    }

    private void generateTransactionReport(ObjectNode reportNode, CommandInput command, Account account, CommandContext context) {
        System.out.println("Business report transaction " + account.getIban());

        ArrayNode managersArray = context.getObjectMapper().createArrayNode();
        ArrayNode employeesArray = context.getObjectMapper().createArrayNode();

        double totalSpent = 0;
        double totalDeposited = 0;

        // Iterăm prin asociații contului
        for (Map.Entry<String, String> entry : account.getAssociates().entrySet()) {
            String email = entry.getKey();
            String role = entry.getValue();
            // System.out.println("email: " + email + " role: " + role);

            User user = User.findUserByEmail(context.getUsers(), email);
            if (user == null) {
                continue;
            }

            List<Transaction> transactions = User.getTransactionsInRange(user,
                    command.getStartTimestamp(), command.getEndTimestamp(), account.getIban());

            double spent = 0;
            double deposited = 0;

            // Calculăm totalul de cheltuieli și depozite pentru utilizator
            for (Transaction transaction : transactions) {
                // System.out.println("transaction type: " + transaction.getType() + " amount: " + transaction.getAmount());
                if (transaction.getType().equals("spending")) {
                    spent += transaction.getAmount();
                } else if (transaction.getType().equals("deposit")) {
                    deposited += transaction.getAmount();
                }
            }

            // Creăm un nod JSON pentru asociat
            ObjectNode associateNode = context.getObjectMapper().createObjectNode();
            associateNode.put("username", user.getLastName() + " " + user.getFirstName());
            associateNode.put("spent", spent);
            associateNode.put("deposited", deposited);

            // Adăugăm nodul în array-ul corespunzător pe baza rolului
            if ("manager".equals(role)) {
                managersArray.add(associateNode);
            } else if ("employee".equals(role)) {
                employeesArray.add(associateNode);
            }

            // Actualizăm totalurile globale
            totalSpent += spent;
            totalDeposited += deposited;
        }

        // Adăugăm rezultatele în nodul raportului
        reportNode.set("managers", managersArray);
        reportNode.set("employees", employeesArray);
        reportNode.put("total spent", totalSpent);
        reportNode.put("total deposited", totalDeposited);
    }

    private void generateCommerciantReport(ObjectNode reportNode, CommandInput command, Account account, CommandContext context) {
        System.out.println("Business report commerciant " + account.getIban());

        // HashMap pentru a organiza datele per comerciant
        Map<String, ObjectNode> commerciantData = new TreeMap<>(); // Sortat alfabetic

        // Iterăm prin asociații contului
        for (Map.Entry<String, String> entry : account.getAssociates().entrySet()) {
            String email = entry.getKey();
            String role = entry.getValue();

            User user = User.findUserByEmail(context.getUsers(), email);
            if (user == null) {
                continue;
            }

            // Obținem tranzacțiile din intervalul de timp
            List<Transaction> transactions = User.getTransactionsInRange(user,
                    command.getStartTimestamp(), command.getEndTimestamp(), account.getIban());

            // Procesăm fiecare tranzacție
            for (Transaction transaction : transactions) {
                if (!transaction.getType().equals("spending")) {
                    continue; // Ignorăm tranzacțiile care nu sunt cheltuieli
                }

                String commerciantName = transaction.getCommerciant();
                if (commerciantName == null || commerciantName.isEmpty()) {
                    continue; // Ignorăm tranzacțiile fără comerciant valid
                }

                // Adăugăm comerciantul în map dacă nu există deja
                commerciantData.putIfAbsent(commerciantName, context.getObjectMapper().createObjectNode());
                ObjectNode commerciantNode = commerciantData.get(commerciantName);

                // Actualizăm suma totală pentru comerciant
                double totalReceived = commerciantNode.has("total received")
                        ? commerciantNode.get("total received").asDouble()
                        : 0;
                commerciantNode.put("total received", totalReceived + transaction.getAmount());

                // Adăugăm utilizatorul în lista corespunzătoare (manager sau employee)
                ArrayNode managersArray = commerciantNode.has("managers")
                        ? (ArrayNode) commerciantNode.get("managers")
                        : context.getObjectMapper().createArrayNode();
                ArrayNode employeesArray = commerciantNode.has("employees")
                        ? (ArrayNode) commerciantNode.get("employees")
                        : context.getObjectMapper().createArrayNode();

                String fullName = user.getLastName() + " " + user.getFirstName();

                // Adăugăm utilizatorul de fiecare dată când există o tranzacție
                if ("manager".equals(role)) {
                    managersArray.add(fullName);
                } else if ("employee".equals(role)) {
                    employeesArray.add(fullName);
                }

                // Actualizăm nodul comerciantului
                commerciantNode.set("managers", managersArray);
                commerciantNode.set("employees", employeesArray);
            }
        }

        // Adăugăm comercianții la raport
        ArrayNode commerciantsArray = context.getObjectMapper().createArrayNode();
        for (Map.Entry<String, ObjectNode> entry : commerciantData.entrySet()) {
            ObjectNode commerciantNode = entry.getValue();
            commerciantNode.put("commerciant", entry.getKey());
            commerciantsArray.add(commerciantNode);
        }

        reportNode.set("commerciants", commerciantsArray);
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
            double rate;
            if (account.getType().equals("savings")) {
                rate = account.getInterestRate() * account.getBalance();
                System.out.println("rate: " + rate);
                account.addFunds(rate);
                System.out.println("Interest added to account " + account.getIban() + " " + account.getBalance() + " " + account.getCurrency());
            } else {
                throw new IllegalArgumentException("This is not a savings account");
            }

            User user = User.findUserByAccount(context.getUsers(), account);
            if (user == null) {
                throw new UserNotFoundException("User not found");
            }

            Transaction transaction = new Transaction.TransactionBuilder(command.getTimestamp(),
                    "Interest rate income", account.getIban(), "create")
                    .amount(rate)
                    .amountCurrency(account.getCurrency())
                    .build();

            System.out.println("amount in transaction: " + transaction.getAmount());

            user.addTransaction(transaction);

        } catch (AccountNotFoundException | IllegalArgumentException | UserNotFoundException e) {
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
                    account.getIban(), "create")
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
        System.out.println("withdrawSavings " + command.getTimestamp());
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
                    "Account is not of type savings.", accountIBAN, "error")
                    .build();
            user.addTransaction(transaction);
            return;
        }

        // Check if the user is of minimum age
        if (!user.isOfMinimumAge(21)) {
            Transaction transaction = new Transaction.TransactionBuilder(command.getTimestamp(),
                    "You don't have the minimum age required.", accountIBAN, "error")
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
                    "You do not have a classic account.", accountIBAN, "error")
                    .build();
            user.addTransaction(transaction);
            return;
        }

        // Check if the account has enough funds
        if (account.getBalance() < amount) {
            Transaction transaction = new Transaction.TransactionBuilder(command.getTimestamp(),
                    "Insufficient funds", accountIBAN, "error")
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
                "Savings withdrawal", accountIBAN, "spending")
                .error(null)
                .amount(amount)
                .classicAccountIBAN(classicAccount.getIban())
                .savingsAccountIBAN(accountIBAN)
                .build();
        user.addTransaction(transaction);
        user.addTransaction(transaction);
//        Transaction savingTransaction = new Transaction.TransactionBuilder(command.getTimestamp(),
//                "Savings withdrawal", accountIBAN, "spending")
//                .amount(amount)
//                .build();
//        user.addTransaction(savingTransaction);
//        user.addTransaction(savingTransaction);
    }

    /**
     * Upgrade the plan of a user. The user can upgrade from a standard or student plan to a silver and
     * @param command the command to be executed
     * @param context the context of the command
     */
    public void upgradePlan(final CommandInput command, final CommandContext context) {
        System.out.println(command.getCommand() + " " + command.getTimestamp());
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
                    "The user already has the " + newPlanType + " plan.", accountIBAN,
                    "error")
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
                    "You cannot downgrade your plan.", accountIBAN, "error")
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
                    "Upgrade plan", accountIBAN, "upgrade")
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
        } catch (CurrencyConversionException e) {
            addError(context.getOutput(), e.getMessage(),
                    command.getTimestamp(), "upgradePlan");
            return;
        }
        System.out.println("convertedFee: " + convertedFee + " " + account.getCurrency());
        double commission = user.getCurrentPlan().calculateTransactionFee(convertedFee);
        System.out.println("commission: " + commission);
        // convertedFee += commission;

        // Check if the account has enough funds for the upgrade
        if (account.getBalance() < convertedFee) {
            Transaction transaction = new Transaction.TransactionBuilder(command.getTimestamp(),
                    "Insufficient funds", accountIBAN, "error")
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
                "Upgrade plan", accountIBAN, "upgrade")
                .currentPlan(user.getCurrentPlan().getPlanType())
                .build();
        user.addTransaction(transaction);
        System.out.println("Balance after upgrade is: " + account.getBalance());
    }

    public void cashWithdrawal(final CommandInput command, final CommandContext context) throws UserNotFoundException,
            CardNotFoundException,
            UnauthorizedCardAccessException,
            InsufficientFundsException,
            UnauthorizedCardStatusException {
        System.out.println(command.getCommand() + " " + command.getTimestamp());
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

            double amountInAccountCurrency;
            amountInAccountCurrency = context.getCurrencyConverter().
                    convertCurrency(finalAmount,
                    "RON",
                    accountUser.getCurrency());

            System.out.println("finalAmountInAccountCurrency: " + amountInAccountCurrency + " " + accountUser.getCurrency());

            // Check if the card is active amd if the account has enough funds
            double newBalance = accountUser.getBalance() - amountInAccountCurrency;
            System.out.println("newBalance: " + newBalance + " " + accountUser.getCurrency());
            System.out.println("minimumBalance: " + accountUser.getMinimumBalance() + " " + accountUser.getCurrency());
            if (newBalance < 0 || newBalance < accountUser.getMinimumBalance()) {
                if (cardUser.getStatus().equals("active")
                        && amountInAccountCurrency > accountUser.getBalance()) {
                    System.out.println("Insufficient funds pentru extragerea de numarar");
                    throw new InsufficientFundsException("Insufficient funds");
                }
            }

            // Make the payment
            accountUser.setBalance(accountUser.getBalance() - amountInAccountCurrency);
            if (cardUser.getStatus().equals("active")
                    && accountUser.getBalance() >= accountUser.getMinimumBalance()) {
                cardUser.setStatus("active");
                System.out.println("S-au extras bani CU SUCCES account balance " + accountUser.getIban() + " plata online: " + accountUser.getBalance() + " " + accountUser.getCurrency());
                Transaction transaction = new Transaction.TransactionBuilder(timestamp,
                        "Cash withdrawal of " + amount, accountUser.getIban(), "spending")
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
        } catch (UserNotFoundException | CardNotFoundException | UnauthorizedCardAccessException |
                 CurrencyConversionException e) {
            addError(context.getOutput(), e.getMessage(), timestamp, "cashWithdrawal");
        } catch (InsufficientFundsException | UnauthorizedCardStatusException e) {
            // Add a transaction to the account
            Transaction transaction = new Transaction.TransactionBuilder(timestamp,
                    e.getMessage(), accountUser.getIban(), "error").build();
            user.addTransaction(transaction);
        } catch (IllegalArgumentException e) {
            // Add an error to the output if the currency conversion is not supported
            addError(context.getOutput(), "Currency conversion not supported",
                    timestamp, "cashWithdrawal");
        }
    }

    public void addNewBusinessAssociate(final CommandInput command, final CommandContext context) {
        Account account = User.findAccountByIBAN(context.getUsers(), command.getAccount());
        if (account == null) {
            addError(context.getOutput(), "Account not found", command.getTimestamp(), "addNewBusinessAssociate");
            return;
        }

        User user = User.findUserByEmail(context.getUsers(), command.getEmail());
        if (user == null) {
            addError(context.getOutput(), "User not found", command.getTimestamp(), "addNewBusinessAssociate");
            return;
        }
        user.setOwnerAccount(account);
        user.setRole(command.getRole());
        account.addAssociate(command.getEmail(), command.getRole());
        System.out.println("Added new associate: " + command.getTimestamp() + " "+ command.getEmail() + " as " + command.getRole() + " to account " + account.getIban());
    }

    public void changeSpendingLimit(final CommandInput command, final CommandContext context) {
        Account account = User.findAccountByIBAN(context.getUsers(), command.getAccount());
        if (account == null) {
            addError(context.getOutput(), "Account not found", command.getTimestamp(), "changeSpendingLimit");
            return;
        }

        if (!account.isAuthorized(command.getEmail(), "changeLimits")) {
            addError(context.getOutput(), "You must be owner in order to change spending limit.",
                    command.getTimestamp(), "changeSpendingLimit");
            return;
        }

        double amountInRON = 0;
        try {
            amountInRON = context.getCurrencyConverter().convertCurrency(command.getAmount(),
                    account.getCurrency(), "RON");
        } catch (CurrencyConversionException e) {
            addError(context.getOutput(), e.getMessage(), command.getTimestamp(), "changeDepositLimit");
            return;
        }
        // Set the deposit limit in RON
        account.setSpendingLimit(amountInRON);
        System.out.println("Spending limit updated to " + command.getAmount() + " " + account.getCurrency() + " = "+  account.getSpendingLimit() + " RON at timestamp " + command.getTimestamp());
    }

    public void changeDepositLimit(final CommandInput command, final CommandContext context) {
        Account account = User.findAccountByIBAN(context.getUsers(), command.getAccount());
        if (account == null) {
            addError(context.getOutput(), "Account not found", command.getTimestamp(), "changeDepositLimit");
            return;
        }

        if (!account.isAuthorized(command.getEmail(), "changeLimits")) {
            addError(context.getOutput(), "You are not authorized to make this transaction.",
                    command.getTimestamp(), "changeDepositLimit");
            return;
        }

        double amountInRON = 0;
        try {
            amountInRON = context.getCurrencyConverter().convertCurrency(command.getAmount(),
                    account.getCurrency(), "RON");
        } catch (CurrencyConversionException e) {
            addError(context.getOutput(), e.getMessage(), command.getTimestamp(), "changeDepositLimit");
            return;
        }
        // Set the deposit limit in RON
        account.setDepositLimit(amountInRON);
        System.out.println("Deposit limit updated to " + command.getAmount() + " " + account.getCurrency() + " = "+ amountInRON + " RON at timestamp " + command.getTimestamp());
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
