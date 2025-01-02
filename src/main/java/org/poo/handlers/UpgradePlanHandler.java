package org.poo.handlers;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.poo.models.Transaction;

public class UpgradePlanHandler implements TransactionHandler {

    @Override
    public void handleTransaction(final Transaction transaction, final ObjectNode transactionJson) {
        transactionJson.put("accountIBAN", transaction.getAccount());
        transactionJson.put("newPlanType", transaction.getCurrentPlan());
    }
}
