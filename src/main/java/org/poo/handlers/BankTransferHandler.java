package org.poo.handlers;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.poo.models.Transaction;

/**
 * This class implements the TransactionHandler interface and
 * encapsulates the logic for handling bank transfer transactions.
 */
public class BankTransferHandler implements TransactionHandler {
    /**
     * Adds the sender and receiver IBANs, transfer amount, and the transfer type
     * to the transaction JSON.
     * @param transaction The transaction to be handled.
     * @param transactionJson The JSON object to which the transaction details will be added.
     */
    @Override
    public void handleTransaction(final Transaction transaction, final ObjectNode transactionJson) {
        transactionJson.put("senderIBAN", transaction.getSenderIBAN());
        transactionJson.put("receiverIBAN", transaction.getReceiverIBAN());
        transactionJson.put("amount", transaction.getAmountCurrency());
        transactionJson.put("transferType", transaction.getTransferType());
    }
}
