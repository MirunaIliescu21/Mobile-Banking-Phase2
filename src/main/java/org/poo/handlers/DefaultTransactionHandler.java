package org.poo.handlers;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.poo.models.Transaction;

/**
 * Default handler for transactions with no specific processing logic.
 */
public class DefaultTransactionHandler implements TransactionHandlerStrategy {
    /**
     *  Adds only basic transaction details (timestamp, description).
     *  Can be extended in the future for additional generic behavior.
     * @param transaction  The transaction object
     * @param transactionJson The JSON representation of the transaction
     */
    @Override
    public void handleTransaction(final Transaction transaction, final ObjectNode transactionJson) {
        // Add basic transaction details
        transactionJson.put("timestamp", transaction.getTimestamp());
        transactionJson.put("description", transaction.getDescription());
    }
}
