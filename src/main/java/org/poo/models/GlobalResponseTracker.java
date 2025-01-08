package org.poo.models;

import java.util.HashMap;
import java.util.Map;

public class GlobalResponseTracker {
    private static final Map<String, Map<SplitPayment, Boolean>> responses = new HashMap<>();

    public static synchronized void addResponse(String account, SplitPayment payment, boolean isAccepted) {
        responses.putIfAbsent(account, new HashMap<>());
        responses.get(account).put(payment, isAccepted);
    }

    public static synchronized boolean hasResponded(String account, SplitPayment payment) {
        return responses.containsKey(account) && responses.get(account).containsKey(payment);
    }

    public static synchronized Boolean getResponse(String account, SplitPayment payment) {
        return responses.containsKey(account) ? responses.get(account).get(payment) : null;
    }

    public static synchronized void removeResponsesForPayment(SplitPayment payment) {
        for (Map<SplitPayment, Boolean> map : responses.values()) {
            map.remove(payment);
        }
    }

    public static Map<Object, Object> getResponsesForPayment(SplitPayment splitPayment) {
        Map<Object, Object> responsesForPayment = new HashMap<>();
        for (Map.Entry<String, Map<SplitPayment, Boolean>> entry : responses.entrySet()) {
            if (entry.getValue().containsKey(splitPayment)) {
                responsesForPayment.put(entry.getKey(), entry.getValue().get(splitPayment));
            }
        }
        return responsesForPayment;
    }
}

