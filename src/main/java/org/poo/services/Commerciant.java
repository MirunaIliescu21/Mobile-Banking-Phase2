package org.poo.services;

import lombok.Data;

@Data
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
     * Choose the cashback strategy
     * @return an instance of the cashback strategy based on the commerciant's cashback strategy.
     */
    public CashbackStrategy getCashbackStrategyInstance() {
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

    @Override
    public String toString() {
        return "Commerciant{" +
                "name='" + name + '\'' +
                ", id=" + id +
                ", account='" + account + '\'' +
                ", type='" + type + '\'' +
                ", cashbackStrategy='" + cashbackStrategy + '\'' +
                '}';
    }
}
