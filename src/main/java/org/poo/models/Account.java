package org.poo.models;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;


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
}
