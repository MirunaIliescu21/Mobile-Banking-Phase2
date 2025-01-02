package org.poo.handlers;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.poo.models.Transaction;

public class CashWithdrawalHandler implements TransactionHandler {

    @Override
    public void handleTransaction(final Transaction transaction, final ObjectNode transactionJson) {
        transactionJson.put("amount", transaction.getAmount());
    }
}
