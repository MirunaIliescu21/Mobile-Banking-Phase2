package org.poo.models;

import lombok.Data;
import lombok.Getter;

@Data
/**
 * The Card class represents a card associated with a bank account.
 */
public class Card {
    @Getter
    private final String cardNumber;
    @Getter
    private String status;
    private String type;

    public Card(final String cardNumber, final String status, final String type) {
        this.cardNumber = cardNumber;
        this.status = status;
        this.type = type;
    }
}
