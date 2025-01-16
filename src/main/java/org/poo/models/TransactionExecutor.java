package org.poo.models;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.poo.handlers.AddFundsHandler;
import org.poo.handlers.BankTransferHandler;
import org.poo.handlers.CreateCardHandler;
import org.poo.handlers.TransactionHandler;
import org.poo.handlers.CardDestroyedHandler;
import org.poo.handlers.CardPaymentHandler;
import org.poo.handlers.SplitPaymentHandler;
import org.poo.handlers.UpgradePlanHandler;
import org.poo.handlers.CashWithdrawalHandler;
import org.poo.handlers.WithdrawalSavingsHandler;
import org.poo.handlers.AddInterestHandler;
import org.poo.handlers.DefaultTransactionHandler;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages the execution of various transaction types using a handler-based architecture.
 *
 * The class maps transaction types ("bankTransfer", "cardPayment") to specific
 * implementations of TransactionHandler which define the behavior for each type.
 * Handles unknown transaction types gracefully by logging an error.
 * Call `executeTransaction` with a transaction type, a `Transaction` object
 * and a JSON representation of the transaction to trigger the appropriate logic.
 */
public abstract class TransactionExecutor {
    private static final Map<String, TransactionHandler> TRANSACTION_HANDLERS = new HashMap<>();

    static {
        TRANSACTION_HANDLERS.put("bankTransfer", new BankTransferHandler());
        TRANSACTION_HANDLERS.put("createCard", new CreateCardHandler());
        TRANSACTION_HANDLERS.put("destroyCard", new CardDestroyedHandler());
        TRANSACTION_HANDLERS.put("cardPayment", new CardPaymentHandler());
        TRANSACTION_HANDLERS.put("splitPayment", new SplitPaymentHandler());
        TRANSACTION_HANDLERS.put("upgradePlan", new UpgradePlanHandler());
        TRANSACTION_HANDLERS.put("cashWithdrawal", new CashWithdrawalHandler());
        TRANSACTION_HANDLERS.put("withdrawalSavings", new WithdrawalSavingsHandler());
        TRANSACTION_HANDLERS.put("addInterest", new AddInterestHandler());
        TRANSACTION_HANDLERS.put("addFunds", new AddFundsHandler());

        // Use DefaultTransactionHandler for non-specific cases
        TransactionHandler defaultHandler = new DefaultTransactionHandler();
        TRANSACTION_HANDLERS.put("insufficientFunds", defaultHandler);
        TRANSACTION_HANDLERS.put("createAccount", defaultHandler);
        TRANSACTION_HANDLERS.put("checkStatusCard", defaultHandler);
    }

    /**
     * Executes the transaction based on the transaction type.
     * @param type the type of the transaction
     * @param transaction the transaction object
     * @param transactionJson the JSON representation of the transaction
     */
    public static void executeTransaction(final String type,
                                          final Transaction transaction,
                                          final ObjectNode transactionJson) {
        TransactionHandler handler = TRANSACTION_HANDLERS.get(type);
        if (handler != null) {
            handler.handleTransaction(transaction, transactionJson);
        } else {
            System.err.println("Unknown transaction type: " + type);
        }
    }
}
