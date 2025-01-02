package org.poo.services;

import org.poo.models.Transaction;
import org.poo.models.User;

/**
 * This strategy offers cashback based on spending thresholds.
 */
public class SpendingThresholdCashback implements CashbackStrategy {
    @Override
    public double calculateCashback(User user, Commerciant commerciant, double totalSpending) {
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

        return totalSpending * cashbackRate;
    }
}
