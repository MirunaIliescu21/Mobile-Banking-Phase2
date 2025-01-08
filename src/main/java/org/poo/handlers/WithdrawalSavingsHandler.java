package org.poo.handlers;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.poo.models.Transaction;

public class WithdrawalSavingsHandler implements TransactionHandler {

    @Override
    public void handleTransaction(final Transaction transaction, final ObjectNode transactionJson) {
        transactionJson.put("amount", transaction.getAmount());
        transactionJson.put("classicAccountIBAN", transaction.getClassicAccountIBAN());
        transactionJson.put("savingsAccountIBAN", transaction.getSavingsAccountIBAN());
    }
}