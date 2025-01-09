package org.poo.services;

import lombok.Data;

@Data
/**
 * Represents a merchant (commerciant) in the system.
 * A commerciant has a name, a unique ID, an account, a type (e.g., food, clothes, tech),
 * and a cashback strategy to reward users for transactions.
 * The cashback strategy determines how cashback is calculated for the merchant's customers.
 * Currently supported strategies are:
 * - "nrOfTransactions": Based on the number of transactions made.
 * - "spendingThreshold": Based on total spending amount.
 */
public class Commerciant {
    private final String name;
    private final int id;
    private final String account;
    private final String type;
    private final String cashbackStrategy;

    public Commerciant(final String name, final int id, final String account,
                       final String type, final String cashbackStrategy) {
        this.name = name;
        this.id = id;
        this.account = account;
        this.type = type;
        this.cashbackStrategy = cashbackStrategy;
    }

    /**
     * Returns an instance of the cashback strategy based on the specified type.
     *
     * @return A CashbackStrategy instance for the merchant.
     * @throws IllegalArgumentException If the cashback strategy is unknown.
     */
    public CashbackStrategy getCashbackStrategyInstance() throws IllegalArgumentException {
        System.out.println("Cashback strategy in class Commerciant: " + this.cashbackStrategy);
        switch (this.cashbackStrategy) {
            case "nrOfTransactions" -> {
                System.out.println("NrOfTransactionsCashback");
                return new NrOfTransactionsCashback();
            }
            case "spendingThreshold" -> {
                System.out.println("SpendingThresholdCashback");
                return new SpendingThresholdCashback();
            }
            default -> {
                System.out.println("Unknown cashback strategy: " + cashbackStrategy);
                throw new IllegalArgumentException("Unknown cashback strategy: " + cashbackStrategy);
            }
        }
    }
}
