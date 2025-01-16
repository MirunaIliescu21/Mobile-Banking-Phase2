package org.poo.commands;

import org.poo.exceptions.CurrencyConversionException;
import org.poo.fileio.CommandInput;
import org.poo.models.Account;
import org.poo.models.Transaction;
import org.poo.models.User;
import org.poo.services.GoldPlan;
import org.poo.services.ServicePlan;
import org.poo.services.SilverPlan;

import java.util.List;

import static org.poo.commands.CommandErrors.addError;

public class UpgradePlanCommand implements Command {
    private static final int FEE_STANDARD_TO_SILVER = 100;
    private static final int FEE_STUDENT_TO_SILVER = 100;
    private static final int FEE_SILVER_TO_GOLD = 250;
    private static final int FEE_STANDARD_TO_GOLD = 350;
    private static final int FEE_STUDENT_TO_GOLD = 350;
    private static final int MIN_TRANSACTIONS = 5;
    private static final int MIN_TRANSACTION_AMOUNT = 300;
    private static final int GOLD_INDEX = 3;
    /**
     * Upgrade the plan of a user. The user can upgrade from a standard or student plan
     * to a silver or gold plan; also from silver to gold plan by paying a fee.
     * @param command the command to be executed
     * @param context the context of the command
     */
    @Override
    public void execute(final CommandInput command, final CommandContext context) {
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

        int approvedTransactions = user.countCardPaymentTransaction(command.getAccount(),
                                                            MIN_TRANSACTION_AMOUNT, context);
        // If the current plan is silver and the user has at least 5 approved transactions
        // of at least 300 RON, the upgrade is made automatically to gold
        if (currentPlanIndex == 2 && approvedTransactions >= MIN_TRANSACTIONS) {
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
        if (currentPlanIndex == 0 && newPlanIndex == 2) {
            fee = FEE_STANDARD_TO_SILVER; // standard to silver
        }
        if (currentPlanIndex == 1 && newPlanIndex == 2) {
            fee = FEE_STUDENT_TO_SILVER; // student to silver
        }
        if (currentPlanIndex == 2 && newPlanIndex == GOLD_INDEX) {
            fee = FEE_SILVER_TO_GOLD; // silver to gold
        }
        if (currentPlanIndex == 0 && newPlanIndex == GOLD_INDEX) {
            fee = FEE_STANDARD_TO_GOLD; // standard to gold
        }
        if (currentPlanIndex == 1 && newPlanIndex == GOLD_INDEX) {
            fee = FEE_STUDENT_TO_GOLD; // student to golf
        }

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
}
