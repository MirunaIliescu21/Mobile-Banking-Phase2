package org.poo.models;

import lombok.Data;

import java.util.*;


@Data
/**
 * The Account class represents a bank account. It has a constructor and methods to add
 * funds to the account, find a card by its card number, add interest to a savings account,
 * and change the interest rate of a savings account.
 */
public class Account {
    private final String iban;
    private double balance;
    private double minimumBalance;
    private final String currency;
    private final String type;
    private final List<Card> cards;
    private final String owner;
    private String alias;
    private double interestRate;
    private LinkedHashMap<String, String> associates = new LinkedHashMap<>(); // Email -> Role
    private double spendingLimit = 500; // Default spending limit in RON
    private double depositLimit = 500; // Default deposit limit in RON

    public Account(final String iban, final String currency,
                   final String type, final String ownerEmail) {
        this.iban = iban;
        balance = 0.0;
        this.currency = currency;
        this.type = type;
        owner = ownerEmail;
        minimumBalance = 0.0;
        cards = new ArrayList<>();
        alias = null;
        interestRate = 0.0;
    }

    /**
     * Find a specific card by its card number.
     * @param card the card number of the card
     */
    public void addCard(final Card card) {
        cards.add(card);
    }

    /**
     * Add funds to the account.
     * @param amount the amount to be added
     */
    public void addFunds(final double amount) {
        balance += amount;
    }

    /**
     * Find a specific card by its card number.
     * @param cardNumber the card number of the card
     * @return the card with the given card number, or null if the card was not found
     */
    public Card findCardByNumber(final String cardNumber) {
        for (Card card : cards) {
            if (card.getCardNumber().equals(cardNumber)) {
                return card;
            }
        }
        return null;
    }

    public boolean isOwner(String email) {
        return owner.equals(email);
    }

    public void addAssociate(String email, String role) {
        associates.put(email, role);
    }

    public boolean isAuthorized(String email, String action) {
        String role = associates.get(email);
        if ("changeLimits".equals(action)) {
            return isOwner(email); // Only the owner can change the limits
        }
        if ("makeTransactions".equals(action)) {
            return "manager".equals(role) || "employee".equals(role);
        }
        return false;
    }
}
