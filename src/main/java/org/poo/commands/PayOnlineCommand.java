package org.poo.commands;

import org.poo.exceptions.*;
import org.poo.fileio.CommandInput;
import org.poo.models.Account;
import org.poo.models.Card;
import org.poo.models.Transaction;
import org.poo.models.User;
import org.poo.services.CashbackStrategy;
import org.poo.services.Commerciant;
import org.poo.services.GoldPlan;
import org.poo.utils.Utils;

import static org.poo.commands.CommandErrors.addError;
import static org.poo.models.User.findUserByEmail;

public class PayOnlineCommand implements Command {
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
    @Override
    public void execute(final CommandInput command,
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
}
