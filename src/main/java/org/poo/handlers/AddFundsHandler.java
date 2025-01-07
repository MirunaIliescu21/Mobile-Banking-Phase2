package org.poo.handlers;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.poo.models.Transaction;

/**
 * Handles the logic for a "addFunds" transaction,
 * but this transaction is not printed in the JSON file.
 */
public class AddFundsHandler implements TransactionHandler {

    @Override
    public void handleTransaction(final Transaction transaction, final ObjectNode transactionJson) {
        transactionJson.put("amount", transaction.getAmount());
    }
}