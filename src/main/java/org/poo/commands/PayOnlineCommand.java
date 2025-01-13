package org.poo.commands;

import org.poo.exceptions.CardNotFoundException;
import org.poo.exceptions.CurrencyConversionException;
import org.poo.exceptions.InsufficientFundsException;
import org.poo.exceptions.UnauthorizedCardAccessException;
import org.poo.exceptions.UserNotFoundException;
import org.poo.exceptions.UnauthorizedCardStatusException;
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
    private static final int SILVER_PAYMENTS_LIMIT = 5;
    private static final int SPENDING_LIMIT_FOR_SILVER = 300;
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
            CardNotFoundException, UnauthorizedCardAccessException,
            InsufficientFundsException, UnauthorizedCardStatusException {
        System.out.println("payOnline " + command.getTimestamp());

        if (command.getAmount() <= 0) {
            return;
        }
        String email = command.getEmail();
        String cardNumber = command.getCardNumber();
        int timestamp = command.getTimestamp();
        User user = findUserByEmail(context.getUsers(), email);
        Card cardUser = null;
        Account accountUser = null;

        try {
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
            // Search for the card in each account of the user
            if (cardUser == null
                    && (!user.getRole().equals("owner") && !user.getRole().equals("user"))) {
                System.out.println("Caut printe cardurile ownerului");
                if (ownerAccount.findCardByNumber(cardNumber) != null) {
                    cardUser = ownerAccount.findCardByNumber(cardNumber);
                    accountUser = ownerAccount;
                }
            }
            // If the card was not found, add an error to the output
            if (cardUser == null) {
                throw new CardNotFoundException("Card not found");
            }

            double amountInRON = context.getCurrencyConverter().
                    convertCurrency(command.getAmount(), command.getCurrency(), "RON");
            System.out.println("amountInRON: " + amountInRON + " RON");

            if (user.getRole().equals("employee")
                    && amountInRON > ownerAccount.getSpendingLimit()) {
                System.out.println("Userul este employee si nu poate sa cheltuie atatia bani");
                return;
            }
            // Check if the user owns the card
            if (!accountUser.getOwner().equals(email) && user.getRole().equals("owner")) {
                throw new UnauthorizedCardAccessException("User does not own the card");
            }
            // Convert the amount to the account currency
            double amountInAccountCurrency = context.getCurrencyConverter()
                                            .convertCurrency(command.getAmount(),
                                            command.getCurrency(), accountUser.getCurrency());
            System.out.println("amountInAccountCurrency: " + amountInAccountCurrency
                                + " " + accountUser.getCurrency());
            // Calculate commission in RON for the silver plan
            double commissionInRON = user.getCurrentPlan().calculateTransactionFee(amountInRON);

            double commission = context.getCurrencyConverter().convertCurrency(commissionInRON,
                            "RON", accountUser.getCurrency());
            System.out.println("commission: " + commission);
            // Calculate cashback
            Commerciant commerciant = context.findCommerciantByName(command.getCommerciant());
            System.out.println("commerciant: " + commerciant.getName()
                                + " " + commerciant.getType());

            CashbackStrategy cashbackStrategy = commerciant.getCashbackStrategyInstance();
            String cashbackType = commerciant.getCashbackStrategy();
            double cashback = cashbackStrategy.calculateCashback(user, commerciant,
                    accountUser, amountInAccountCurrency, context);
            System.out.println("cashbacktype: " + cashbackType + " cashback: " + cashback);

            // Apply commission and cashback
            double finalAmount = amountInAccountCurrency + commission - cashback;
            System.out.println("finalAmount: " + finalAmount + " " + accountUser.getCurrency());

            // Check if the card is active amd if the account has enough funds
            double newBalance = accountUser.getBalance() - finalAmount;
            if (newBalance < accountUser.getMinimumBalance()) {
                if (cardUser.getStatus().equals("active")
                        && finalAmount > accountUser.getBalance()) {
                    System.out.println("Insufficient funds at timestamp "
                                        + command.getTimestamp());
                    throw new InsufficientFundsException("Insufficient funds");
                }
            }
            // Make the payment
            accountUser.setBalance(accountUser.getBalance() - finalAmount);
            if (cardUser.getStatus().equals("active")
                    && accountUser.getBalance() >= accountUser.getMinimumBalance()) {
                cardUser.setStatus("active");
                System.out.println("S-A EFECTUAT TRANZACTIA CU SUCCES account balance "
                        + accountUser.getIban() + " plata online: "
                        + accountUser.getBalance() + " " + accountUser.getCurrency());

                Transaction transaction = new Transaction.TransactionBuilder(timestamp,
                        "Card payment", accountUser.getIban(), "spending")
                        .amount((amountInAccountCurrency))
                        .amountCurrency(accountUser.getCurrency())
                        .commerciant(command.getCommerciant())
                        .build();
                user.addTransaction(transaction);

                // Cashback exists, and it's the type `spendingThreshold`,
                // the total spending for this account is updated
                if (cashbackType.equals("spendingThreshold")) {
                    double oldSpending = accountUser.getSpendingThreshold();
                    accountUser.setSpendingThreshold(oldSpending + amountInRON);
                }

                countSilverPayments(amountInRON, user, accountUser, command);

                // If the card is the type of "one time pay",
                // it is destroyed after the payment and a new card is created
                if (cardUser.getType().equals("one time pay")) {
                    deleteAndCreateNewCard(cardUser, user, accountUser, command);
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
     * If the payment is made with a one time card, the card needs to be destroyed (deleted)
     *  after the payment and create a new one.
     *  This method delete and create another one time card.
     * @param cardUser the card of the current user
     * @param user the user that made the payment
     * @param accountUser the user's account
     * @param command the command from input
     */
    private static void deleteAndCreateNewCard(final Card cardUser,
                                               final User user, final Account accountUser,
                                               final CommandInput command) {
        int timestamp = command.getTimestamp();
        String cardNumber =  command.getCardNumber();

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

    /**
     * Check if the user has the silver plan and makes a payment grater than 300 RON.
     * If the user has minim. 5 payments like this, it will be made
     * an automatic update to the gold plan
     * @param amountInRON the amount in RON to compare with 300 ron
     * @param user the user that makes the payment
     * @param accountUser the account
     * @param command the command from the input
     */
    public static void countSilverPayments(final double amountInRON, final User user,
                                            final Account accountUser, final CommandInput command) {
        if (amountInRON > SPENDING_LIMIT_FOR_SILVER
                && user.getCurrentPlan().getPlanType().equals("silver")) {
            user.setCountSilverPayments(user.getCountSilverPayments() + 1);
        }

        if (user.getCountSilverPayments() >= SILVER_PAYMENTS_LIMIT) {
            System.out.println("Userul a efectuat 5 plati > 300 RON + upgrade automant");
            user.setCurrentPlan(new GoldPlan());
            user.setCountSilverPayments(0);
            Transaction upgradeTransaction = new Transaction
                    .TransactionBuilder(command.getTimestamp(),
                    "Upgrade plan", accountUser.getIban(), "upgrade")
                    .currentPlan(user.getCurrentPlan().getPlanType())
                    .build();
            user.addTransaction(upgradeTransaction);
        }
    }
}
