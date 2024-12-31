package org.poo.services;

import lombok.Getter;

@Getter
/**
 * The ExchangeRate class represents an exchange rate between two currencies.
 */
public class ExchangeRate {
    private final String from;
    private final String to;
    private final double rate;

    public ExchangeRate(final String from, final String to, final double rate) {
        this.from = from;
        this.to = to;
        this.rate = rate;
    }
}

