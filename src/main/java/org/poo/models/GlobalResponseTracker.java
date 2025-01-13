package org.poo.models;

import java.util.HashMap;
import java.util.Map;

public abstract class GlobalResponseTracker {
    private static final Map<String, Map<SplitPayment, Boolean>> RESPONSES = new HashMap<>();

    public static synchronized void addResponse(final String account,
                                                final SplitPayment payment,
                                                final boolean isAccepted) {
        RESPONSES.putIfAbsent(account, new HashMap<>());
        RESPONSES.get(account).put(payment, isAccepted);
    }

    public static synchronized boolean hasResponded(final String account,
                                                    final SplitPayment payment) {
        return RESPONSES.containsKey(account) && RESPONSES.get(account).containsKey(payment);
    }

    public static synchronized Boolean getResponse(final String account,
                                                   final SplitPayment payment) {
        return RESPONSES.containsKey(account) ? RESPONSES.get(account).get(payment) : null;
    }

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

