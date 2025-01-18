package org.poo.services;

import org.poo.commands.CommandContext;
import org.poo.exceptions.CurrencyConversionException;
import org.poo.models.Account;
import org.poo.models.User;
import org.poo.utils.CashbackRates;

public class SpendingThresholdCashback implements CashbackStrategy {
    private static final double FIRST_SPENDING_LIMIT = 100;
    private static final double SECOND_SPENDING_LIMIT = 300;
    private static final double THIRD_SPENDING_LIMIT = 500;

    /**
     * Implements cashback calculation based on total spending thresholds.
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

        double totalSpending = account.getSpendingThreshold() + amountInRON;
        System.out.println("Total spending: " + totalSpending + " RON");

        CashbackRates cashbackRate = CashbackRates.DEFAULT;

        if (totalSpending >= THIRD_SPENDING_LIMIT) {
            cashbackRate = switch (user.getCurrentPlan().getPlanType()) {
                case "gold" -> CashbackRates.GOLD_HIGH;
                case "silver" -> CashbackRates.SILVER_HIGH;
                case "standard", "student" -> CashbackRates.STANDARD_STUDENT_HIGH;
                default -> CashbackRates.DEFAULT;
            };
        } else if (totalSpending >= SECOND_SPENDING_LIMIT) {
            cashbackRate = switch (user.getCurrentPlan().getPlanType()) {
                case "gold" -> CashbackRates.GOLD_MEDIUM;
                case "silver" -> CashbackRates.SILVER_MEDIUM;
                case "standard", "student" -> CashbackRates.STANDARD_STUDENT_MEDIUM;
                default -> CashbackRates.DEFAULT;
            };
        } else if (totalSpending >= FIRST_SPENDING_LIMIT) {
            cashbackRate = switch (user.getCurrentPlan().getPlanType()) {
                case "gold" -> CashbackRates.GOLD_LOW;
                case "silver" -> CashbackRates.SILVER_LOW;
                case "standard", "student" -> CashbackRates.STANDARD_STUDENT_LOW;
                default -> CashbackRates.DEFAULT;
            };
        }

        return spendingAmount * cashbackRate.getRate();
    }
}
