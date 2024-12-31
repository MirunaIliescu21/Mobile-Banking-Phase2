package org.poo.handlers;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.poo.models.Transaction;

/**
 * This class implements the TransactionHandler interface and
 * encapsulates the logic for handling card payment transactions.
 */
public class CardPaymentHandler implements TransactionHandler {
    /**
     * Includes the payment amount and the merchant's details in the transaction JSON.
     * @param transaction The transaction to be handled.
     * @param transactionJson The JSON object to which the transaction details will be added.
     */
    @Override
    public void handleTransaction(final Transaction transaction, final ObjectNode transactionJson) {
        transactionJson.put("amount", transaction.getAmount());
        transactionJson.put("commerciant", transaction.getCommerciant());
    }
}
