package org.poo.commands;

/**
 * Enum representing the command types.
 */
public enum CommandType {
    PRINT_USERS("printUsers"),
    ADD_ACCOUNT("addAccount"),
    CREATE_CARD("createCard"),
    CREATE_ONE_TIME_CARD("createOneTimeCard"),
    ADD_FUNDS("addFunds"),
    DELETE_ACCOUNT("deleteAccount"),
    DELETE_CARD("deleteCard"),
    PAY_ONLINE("payOnline"),
    SEND_MONEY("sendMoney"),
    SET_ALIAS("setAlias"),
    PRINT_TRANSACTIONS("printTransactions"),
    SET_MINIMUM_BALANCE("setMinimumBalance"),
    CHECK_CARD_STATUS("checkCardStatus"),
    SPLIT_PAYMENT("splitPayment"),
    REPORT("report"),
    SPENDINGS_REPORT("spendingsReport"),
    BUSINESS_REPORT("businessReport"),
    ADD_INTEREST("addInterest"),
    CHANGE_INTEREST_RATE("changeInterestRate"),
    WITHDRAW_SAVINGS("withdrawSavings"),
    UPGRADE_PLAN("upgradePlan"),
    CASH_WITHDRAWAL("cashWithdrawal"),
    ACCEPT_SPLIT_PAYMENT("acceptSplitPayment"),
    REJECT_SPLIT_PAYMENT("rejectSplitPayment"),
    ADD_BUSINESS_ASSOCIATE("addNewBusinessAssociate"),
    CHANGE_SPENDING_LIMIT("changeSpendingLimit"),
    CHANGE_DEPOSIT_LIMIT("changeDepositLimit"),
    DEFAULT("default");

    private final String commandName;

    CommandType(final String commandName) {
        this.commandName = commandName;
    }

    /**
     * Returns the command name.
     * @param commandName the command name that is searched.
     * @return the command type.
     */
    public static CommandType fromCommandName(final String commandName) {
        for (CommandType type : values()) {
            if (type.commandName.equals(commandName)) {
                return type;
            }
        }
        return DEFAULT;
    }
}

