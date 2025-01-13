package org.poo.models;

import lombok.Data;
import lombok.Getter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;


@Data
/**
 * The Account class represents a bank account. It has a constructor and methods to add
 * funds to the account, find a card by its card number, add interest to a savings account,
 * and change the interest rate of a savings account.
 * And also, for the second phase, this class contains new methods for adding associates
 * to a business account and verify the owner.
 */
@Getter
public class Account {
    private static final double INITIAL_LIMIT = 500; // in RON
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
    private double spendingLimit = INITIAL_LIMIT; // Default spending limit in RON
    private double depositLimit = INITIAL_LIMIT; // Default deposit limit in RON
    private double spendingThreshold;

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
        spendingThreshold = 0.0;
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

    /**
     * Verify if the email is an owner's email
     * @param email the current email
     * @return true if the user with this email is an owner
     */
    public boolean isOwner(final String email) {
        return owner.equals(email);
    }

    /**
     * Add an associate for a business account.
     * @param email the email of the associate
     * @param role the associate's role (employee or manager)
     */
    public void addAssociate(final String email, final String role) {
        associates.put(email, role);
    }

    /**
     * Check if the user is authorized to do the current operation
     * @param email the user's email
     * @param action the name of operation
     * @return true if it is authorized, false otherwise
     */
    public boolean isAuthorized(final String email, final String action) {
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
