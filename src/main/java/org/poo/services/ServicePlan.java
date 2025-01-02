package org.poo.services;

import org.poo.models.Account;

public interface ServicePlan {
    String getPlanType();
    double calculateTransactionFee(double amount);
    boolean qualifiesForUpgrade(Account account);
}

