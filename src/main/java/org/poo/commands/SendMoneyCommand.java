package org.poo.commands;

import org.poo.exceptions.AccountNotFoundException;
import org.poo.exceptions.CurrencyConversionException;
import org.poo.exceptions.InsufficientFundsException;
import org.poo.exceptions.UserNotFoundException;
import org.poo.fileio.CommandInput;
import org.poo.models.Account;
import org.poo.models.Transaction;
import org.poo.models.User;
import org.poo.services.CashbackStrategy;
import org.poo.services.Commerciant;
import org.poo.services.GoldPlan;

import static org.poo.commands.CommandErrors.addError;
import static org.poo.models.User.findUserByEmail;

public class SendMoneyCommand implements Command {
    /**
     * Send money from one account to another.
     * Add a transaction to the sender and the receiver.
     * @param command Command input containing email, card number, amount, and currency.
     * @param context Command context containing the list of users and the currency converter.
     */
    @Override
    public void execute(final CommandInput command,
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
                System.out.println("Comanda sendMoney de la timestamp: " + command.getTimestamp()
                        + " functioneaza ca o plata online catre un comerciant");
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
}
