package org.poo.services;

import org.poo.commands.CommandContext;
import org.poo.exceptions.CurrencyConversionException;
import org.poo.models.User;


public class SpendingThresholdCashback implements CashbackStrategy {
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
                                    final String accountCurrency, final double spendingAmount,
                                    final CommandContext context)
                                    throws CurrencyConversionException {
        System.out.println("calculateCashback for SpendingThresholdCashback");
        double amountInRON = 0;
        try {
            amountInRON = context.getCurrencyConverter().convertCurrency(spendingAmount, accountCurrency, "RON");
        } catch (CurrencyConversionException e) {
            System.out.println("Currency conversion failed: " + e.getMessage());
            return 0;
        }

        double totalSpending = amountInRON + user.getTotalSpending(commerciant, context);
        System.out.println("Total spending: " + totalSpending + " RON");

        double cashbackRate = 0;

        if (totalSpending >= 500) {
            switch (user.getCurrentPlan().getPlanType()) {
                case "gold" -> cashbackRate = 0.007;
                case "silver" -> cashbackRate = 0.005;
                case "standard", "student" -> cashbackRate = 0.0025;
            }
        } else if (totalSpending >= 300) {
            switch (user.getCurrentPlan().getPlanType()) {
                case "gold" -> cashbackRate = 0.0055;
                case "silver" -> cashbackRate = 0.004;
                case "standard", "student" -> cashbackRate = 0.002;
            }
        } else if (totalSpending >= 100) {
            switch (user.getCurrentPlan().getPlanType()) {
                case "gold" -> cashbackRate = 0.005;
                case "silver" -> cashbackRate = 0.003;
                case "standard", "student" -> cashbackRate = 0.001;
            }
        }

        return spendingAmount * cashbackRate;
    }
}
