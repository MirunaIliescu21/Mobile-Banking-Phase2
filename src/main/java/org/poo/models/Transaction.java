package org.poo.models;

import lombok.Data;
import java.util.List;

@Data
/**
 * Represents a financial transaction performed by a user.
 * Each transaction has mandatory fields: timestamp, description, account
 * and the rest of them  are optional fields to accommodate different types of transactions.
 *
 * The class uses a Builder design pattern (`TransactionBuilder`) to provide a flexible way
 * of constructing transaction objects with various combinations of optional attributes.
 */
public final class Transaction {
    private final int timestamp;
    private final String description;
    private final String senderIBAN;
    private final String receiverIBAN;
    private final String account;
    private final double amount;
    private final String amountCurrency;
    private final String transferType;
    private final String card;
    private final String cardHolder;
    private final String commerciant;
    private final String currentPlan;
    private String error;
    private List<String> involvedAccounts = null;

    private Transaction(final TransactionBuilder builder) {
        this.timestamp = builder.timestamp;
        this.description = builder.description;
        this.senderIBAN = builder.senderIBAN;
        this.receiverIBAN = builder.receiverIBAN;
        this.account = builder.account;
        this.amount = builder.amount;
        this.amountCurrency = builder.amountCurrency;
        this.transferType = builder.transferType;
        this.card = builder.card;
        this.cardHolder = builder.cardHolder;
        this.commerciant = builder.commerciant;
        this.involvedAccounts = builder.involvedAccounts;
        this.error = builder.error;
        this.currentPlan = builder.currentPlan;
    }

    public static class TransactionBuilder {
        private final int timestamp;
        private final String description;
        private String senderIBAN;
        private String receiverIBAN;
        private String account;
        private String amountCurrency;
        private double amount;
        private String transferType;
        private String card;
        private String cardHolder;
        private String commerciant;
        private String currentPlan;
        private List<String> involvedAccounts;
        private String error;

        public TransactionBuilder(final int timestamp,
                                  final String description,
                                  final String iban) {
            this.timestamp = timestamp;
            this.description = description;
            account = iban;
        }

        /**
         * Set the sender IBAN for the transaction.
         * @param iban The sender IBAN
         * @return The transaction builder
         */
        public TransactionBuilder senderIBAN(final String iban) {
            senderIBAN = iban;
            return this;
        }

        /**
         * Set the receiver IBAN for the transaction.
         * @param iban The receiver IBAN
         * @return The transaction builder
         */
        public TransactionBuilder receiverIBAN(final String iban) {
            receiverIBAN = iban;
            return this;
        }

        /**
         * Set the amount and currency for the transaction.
         * @param newAmount The amount
         * @return The transaction builder
         */
        public TransactionBuilder amountCurrency(final String newAmount) {
            amountCurrency = newAmount;
            return this;
        }

        /**
         * Set the amount for the transaction.
         * @param newAmount The amount
         * @return The transaction builder
         */
        public TransactionBuilder amount(final double newAmount) {
            amount = newAmount;
            return this;
        }

        /**
         * Set the transfer type for the transaction.
         * @param typeTransfer The transfer type
         * @return The transaction builder
         */
        public TransactionBuilder transferType(final String typeTransfer) {
            transferType = typeTransfer;
            return this;
        }

        /**
         * Set the card for the transaction.
         * @param cardNumber The card
         * @return The transaction builder
         */
        public TransactionBuilder card(final String cardNumber) {
            card = cardNumber;
            return this;
        }

        /**
         * Set the card holder for the transaction.
         * @param cardHolderName The card holder
         * @return The transaction builder
         */
        public TransactionBuilder cardHolder(final String cardHolderName) {
            cardHolder = cardHolderName;
            return this;
        }

        /**
         * Set the commerciant for the transaction.
         * @param commerciantName The commerciant
         * @return The transaction builder
         */
        public TransactionBuilder commerciant(final String commerciantName) {
            commerciant = commerciantName;
            return this;
        }

        /**
         * Set the involved accounts for the transaction.
         * @param accounts The list of involved accounts
         * @return The transaction builder
         */
        public TransactionBuilder involvedAccounts(final List<String> accounts) {
            involvedAccounts = accounts;
            return this;
        }

        /**
         * Set the error for the transaction.
         * @param errorMessage The error message
         * @return The transaction builder
         */
        public TransactionBuilder error(final String errorMessage) {
            error = errorMessage;
            return this;
        }

        /**
         * Set the current plan for the transaction.
         * @param plan The current plan
         * @return The transaction builder
         */
        public TransactionBuilder currentPlan(final String plan) {
            currentPlan = plan;
            return this;
        }

        /**
         * Build the transaction object.
         */
        public Transaction build() {
            return new Transaction(this);
        }
    }
}
