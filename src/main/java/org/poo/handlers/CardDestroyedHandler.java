package org.poo.handlers;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.poo.models.Transaction;

/**
 * This class implements the TransactionHandler interface and
 * encapsulates the logic for handling card destruction transactions.
 */
public class CardDestroyedHandler implements TransactionHandler {
    /**
     * Adds the involved card (card) to the transaction JSON.
     * Includes the cardholder and account of the transaction.
     * @param transaction The transaction to be handled.
     * @param transactionJson The JSON object to which the transaction details will be added.
     */
    @Override
    public void handleTransaction(final Transaction transaction, final ObjectNode transactionJson) {
        transactionJson.put("account", transaction.getAccount());
        transactionJson.put("card", transaction.getCard());
        transactionJson.put("cardHolder", transaction.getCardHolder());
    }
}
