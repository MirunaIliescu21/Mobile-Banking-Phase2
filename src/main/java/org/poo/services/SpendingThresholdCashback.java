package org.poo.services;

import org.poo.commands.CommandContext;
import org.poo.exceptions.CurrencyConversionException;
import org.poo.models.Account;
import org.poo.models.User;

public class SpendingThresholdCashback implements CashbackStrategy {
    private static final double FIRST_SPENDING_LIMIT = 100;
    private static final double SECOND_SPENDING_LIMIT = 300;
    private static final double THIRD_SPENDING_LIMIT = 500;

    /**
     * Implements cashback calculation based on total spending thresholds.
     *
     * Cashback rates vary based on the user's subscription plan and spending amount:
     * - Spending >= 500 RON: Higher rates for Gold and Silver plans.
     * - Spending >= 300 RON: Medium rates for all plans.
     * - Spending >= 100 RON: Low rates for all plans.
     */
    @Override
    public double calculateCashback(final User user, final Commerciant commerciant,
                                    final Account account,
                                    final double spendingAmount,
                                    final CommandContext context)
                                    throws CurrencyConversionException {
        System.out.println("calculateCashback for SpendingThresholdCashback");
        String accountCurrency = account.getCurrency();
        double amountInRON = 0;
        try {
            amountInRON = context.getCurrencyConverter()
                    .convertCurrency(spendingAmount, accountCurrency, "RON");
        } catch (CurrencyConversionException e) {
            System.out.println("Currency conversion failed: " + e.getMessage());
            return 0;
        }
        // The total spending for the current account
        double totalSpending =  account.getSpendingThreshold() + amountInRON;
        System.out.println("Total spending: " + totalSpending + " RON");

        double cashbackRate = 0;

        if (totalSpending >= THIRD_SPENDING_LIMIT) {
            switch (user.getCurrentPlan().getPlanType()) {
                case "gold" -> cashbackRate = 0.007;
                case "silver" -> cashbackRate = 0.005;
                case "standard", "student" -> cashbackRate = 0.0025;
                default -> cashbackRate = 0;
            }
        } else if (totalSpending >= SECOND_SPENDING_LIMIT) {
            switch (user.getCurrentPlan().getPlanType()) {
                case "gold" -> cashbackRate = 0.0055;
                case "silver" -> cashbackRate = 0.004;
                case "standard", "student" -> cashbackRate = 0.002;
                default -> cashbackRate = 0;
            }
        } else if (totalSpending >= FIRST_SPENDING_LIMIT) {
            switch (user.getCurrentPlan().getPlanType()) {
                case "gold" -> cashbackRate = 0.005;
                case "silver" -> cashbackRate = 0.003;
                case "standard", "student" -> cashbackRate = 0.001;
                default -> cashbackRate = 0;
            }
        }
        return spendingAmount * cashbackRate;
    }
}
