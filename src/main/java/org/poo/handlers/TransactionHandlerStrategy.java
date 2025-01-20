package org.poo.handlers;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.poo.models.Transaction;

/**
 * Each implementation of this interface encapsulates
 * the logic for handling a particular transaction type.
 */
public interface TransactionHandlerStrategy {
    /**
     * This method promotes single responsibility by delegating transactions to separate handlers.
     * Enables extensibility by allowing new handlers to be added without modifying existing code.
     * @param transaction The transaction to be handled
     * @param transactionJson The JSON object to which the transaction details will be added
     */
    void handleTransaction(Transaction transaction, ObjectNode transactionJson);
}
