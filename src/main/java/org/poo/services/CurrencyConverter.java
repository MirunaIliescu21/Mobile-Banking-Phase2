package org.poo.services;

import org.poo.exceptions.CurrencyConversionException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.LinkedList;

/**
 * The CurrencyConverter class is responsible for converting
 * an amount from one currency to another.
 */
public class CurrencyConverter {

    private final List<ExchangeRate> exchangeRates;

    public CurrencyConverter(final List<ExchangeRate> exchangeRates) {
        this.exchangeRates = exchangeRates;
    }

    /**
     * Convert an amount from one currency to another.
     * The search algorithm must use both direct and inverse relationships.
     * @param amount the amount to be converted
     * @param fromCurrency the currency to convert from
     * @param toCurrency the currency to convert to
     * @return the converted amount or an exception if the conversion is not possible
     */
    public double convertCurrency(final double amount,
                                  final String fromCurrency,
                                  final String toCurrency)
            throws IllegalArgumentException, CurrencyConversionException {
        // If the currencies are the same, return the amount
        if (fromCurrency.equals(toCurrency)) {
            return amount;
        }

        // Map to track visited currencies and their respective conversion rates
        Map<String, Double> visited = new HashMap<>();
        Queue<String> queue = new LinkedList<>();

        // Start with the source currency
        queue.add(fromCurrency);
        visited.put(fromCurrency, 1.0);

        while (!queue.isEmpty()) {
            String current = queue.poll();
            double currentRate = visited.get(current);

            // Traverse all exchange rates
            for (ExchangeRate rate : exchangeRates) {
                // Handle the direct conversion (current -> nextCurrency)
                if (rate.getFrom().equals(current)) {
                    String nextCurrency = rate.getTo();
                    double newRate = currentRate * rate.getRate();

                    if (nextCurrency.equals(toCurrency)) {
                        return amount * newRate;
                    }

                    if (!visited.containsKey(nextCurrency)) {
                        visited.put(nextCurrency, newRate);
                        queue.add(nextCurrency);
                    }
                }

                // Handle the inverse conversion (current -> fromCurrency)
                if (rate.getTo().equals(current)) {
                    String nextCurrency = rate.getFrom();
                    double newRate = currentRate * (1 / rate.getRate());

                    if (nextCurrency.equals(toCurrency)) {
                        return amount * newRate;
                    }

                    if (!visited.containsKey(nextCurrency)) {
                        visited.put(nextCurrency, newRate);
                        queue.add(nextCurrency);
                    }
                }
            }
        }

        // If no path was found, throw an exception
        throw new CurrencyConversionException("Currency conversion not supported");
    }

    /**
     * Helper method: Print all exchange rates.
     */
    public void printExchangeRates() {
        for (ExchangeRate rate : exchangeRates) {
            System.out.println(rate);
        }
    }
}
