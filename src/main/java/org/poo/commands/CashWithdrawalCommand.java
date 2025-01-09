package org.poo.commands;

import org.poo.exceptions.*;
import org.poo.fileio.CommandInput;
import org.poo.models.Account;
import org.poo.models.Card;
import org.poo.models.Transaction;
import org.poo.models.User;

import static org.poo.commands.CommandErrors.addError;
import static org.poo.models.User.findUserByEmail;

public class CashWithdrawalCommand implements Command {
    /**
     * Executes the cash withdrawal command.
     * The user withdraws money from an ATM.
     * @param command the command input containing details for execution
     * @param context the context of the command, including users and services
     * @throws UserNotFoundException if the user does not exist
     * @throws CardNotFoundException if the card is not found
     * @throws UnauthorizedCardAccessException if the user is not authorized to access the card
     * @throws InsufficientFundsException if there are insufficient funds in the account
     * @throws UnauthorizedCardStatusException if the card status is invalid
     */
    @Override
    public void execute(final CommandInput command, final CommandContext context) throws UserNotFoundException,
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


}
