package org.poo.models;

import java.util.HashMap;
import java.util.Map;

public abstract class GlobalResponseTracker {
    private static final Map<String, Map<SplitPayment, Boolean>> RESPONSES = new HashMap<>();

    /**
     * Adds a response to the tracker.
     * @param account the account that responded
     * @param payment the payment that was responded to
     * @param isAccepted whether the payment was accepted or not
     */
    public static synchronized void addResponse(final String account,
                                                final SplitPayment payment,
                                                final boolean isAccepted) {
        RESPONSES.putIfAbsent(account, new HashMap<>());
        RESPONSES.get(account).put(payment, isAccepted);
    }

    /**
     * Checks if an account has responded to a payment.
     * @param account the account to check
     * @param payment the payment to check
     * @return whether the account has responded to the payment
     */
    public static synchronized boolean hasResponded(final String account,
                                                    final SplitPayment payment) {
        return RESPONSES.containsKey(account) && RESPONSES.get(account).containsKey(payment);
    }

    /**
     * Gets the response of an account to a payment.
     * @param account the account to get the response of
     * @param payment the payment to get the response of
     * @return whether the account accepted the payment
     */
    public static synchronized Boolean getResponse(final String account,
                                                   final SplitPayment payment) {
        return RESPONSES.containsKey(account) ? RESPONSES.get(account).get(payment) : null;
    }

    /**
     * Gets the responses for a payment.
     * @param splitPayment the payment to get the responses for
     * @return a map of accounts to their responses
     */
    public static Map<Object, Object> getResponsesForPayment(final SplitPayment splitPayment) {
        Map<Object, Object> responsesForPayment = new HashMap<>();
        for (Map.Entry<String, Map<SplitPayment, Boolean>> entry : RESPONSES.entrySet()) {
            if (entry.getValue().containsKey(splitPayment)) {
                responsesForPayment.put(entry.getKey(), entry.getValue().get(splitPayment));
            }
        }
        return responsesForPayment;
    }
}
