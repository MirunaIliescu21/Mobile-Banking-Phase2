package org.poo.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.poo.models.Transaction;

/**
 * This class implements the TransactionHandler interface and
 * encapsulates the logic for handling split payment transactions.
 */
public class SplitPaymentHandler implements TransactionHandler {
    /**
     * Adds the involved accounts (involvedAccounts) to the transaction JSON.
     * Includes the currency and amount of the transaction.
     * Adds an error message to the output if the transaction contains an error.
     * @param transaction The transaction to be handled.
     * @param transactionJson The JSON object to which the transaction details will be added.
     */
    @Override
    public void handleTransaction(final Transaction transaction, final ObjectNode transactionJson) {
        if (transaction.getError() != null) {
            transactionJson.put("error", transaction.getError());
        }
        transactionJson.put("currency", transaction.getAmountCurrency());
        transactionJson.put("amount", transaction.getAmount());

        ObjectMapper mapper = new ObjectMapper();
        ArrayNode involvedAccountsJson = mapper.createArrayNode();
        for (String iban : transaction.getInvolvedAccounts()) {
            involvedAccountsJson.add(iban);
        }
        transactionJson.set("involvedAccounts", involvedAccountsJson);
    }
}
