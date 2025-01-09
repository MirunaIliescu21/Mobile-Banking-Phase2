package org.poo.handlers;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.poo.models.Transaction;

/**
 * Handles the logic for a "addFunds" transaction,
 * but this transaction is not printed in the JSON file.
 */
public class AddFundsHandler implements TransactionHandler {
    /**
     * Handles the logic for a "addFunds" transaction.
     * This transaction is not printed in the JSON file.
     * @param transaction the transaction to be handled
     * @param transactionJson the JSON object to be printed
     */
    @Override
    public void handleTransaction(final Transaction transaction, final ObjectNode transactionJson) {
        transactionJson.put("amount", transaction.getAmount());
    }
}