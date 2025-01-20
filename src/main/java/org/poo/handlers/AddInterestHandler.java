package org.poo.handlers;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.poo.models.Transaction;

public class AddInterestHandler implements TransactionHandlerStrategy {
    /**
     * Adds the transaction details to the JSON object: amount, currency
     * @param transaction The transaction to be handled
     * @param transactionJson The JSON object to which the transaction details will be added
     */
    @Override
    public void handleTransaction(final Transaction transaction, final ObjectNode transactionJson) {
        transactionJson.put("amount", transaction.getAmount());
        transactionJson.put("currency", transaction.getAmountCurrency());
    }
}
