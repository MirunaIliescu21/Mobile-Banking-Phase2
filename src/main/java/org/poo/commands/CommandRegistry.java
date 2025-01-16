package org.poo.commands;

import java.util.HashMap;
import java.util.Map;

public abstract class CommandRegistry {
    private static final Map<CommandType, Command> COMMANDS = new HashMap<>();

    // Private constructor to prevent instantiation
    private CommandRegistry() {
        throw new UnsupportedOperationException("Utility class");
    }

    static {
        COMMANDS.put(CommandType.PRINT_USERS, new PrintUsersCommand());
        COMMANDS.put(CommandType.CREATE_CARD, new CreateCardCommand());
        COMMANDS.put(CommandType.CREATE_ONE_TIME_CARD, new CreateOneTimeCardCommand());
        COMMANDS.put(CommandType.ADD_ACCOUNT, new AddAccountCommand());
        COMMANDS.put(CommandType.ADD_FUNDS, new AddFundsCommand());
        COMMANDS.put(CommandType.DELETE_ACCOUNT, new DeleteAccountCommand());
        COMMANDS.put(CommandType.PAY_ONLINE, new PayOnlineCommand());
        COMMANDS.put(CommandType.SEND_MONEY, new SendMoneyCommand());
        COMMANDS.put(CommandType.SET_ALIAS, new SetAliasCommand());
        COMMANDS.put(CommandType.DELETE_CARD, new DeleteCardCommand());
        COMMANDS.put(CommandType.PRINT_TRANSACTIONS, new PrintTransactionsCommand());
        COMMANDS.put(CommandType.SET_MINIMUM_BALANCE, new SetMinimumBalanceCommand());
        COMMANDS.put(CommandType.CHECK_CARD_STATUS, new CheckCardStatusCommand());
        COMMANDS.put(CommandType.SPLIT_PAYMENT, new SplitPaymentCommand());
        COMMANDS.put(CommandType.REPORT, new ReportCommand());
        COMMANDS.put(CommandType.SPENDINGS_REPORT, new SpendingsReportCommand());
        COMMANDS.put(CommandType.BUSINESS_REPORT, new BusinessReportCommand());
        COMMANDS.put(CommandType.ADD_INTEREST, new AddInterestCommand());
        COMMANDS.put(CommandType.CHANGE_INTEREST_RATE, new ChangeInterestRateCommand());
        COMMANDS.put(CommandType.WITHDRAW_SAVINGS, new WithdrawSavingsCommand());
        COMMANDS.put(CommandType.UPGRADE_PLAN, new UpgradePlanCommand());
        COMMANDS.put(CommandType.CASH_WITHDRAWAL, new CashWithdrawalCommand());
        COMMANDS.put(CommandType.ACCEPT_SPLIT_PAYMENT, new AcceptSplitPaymentCommand());
        COMMANDS.put(CommandType.REJECT_SPLIT_PAYMENT, new RejectSplitPaymentCommand());
        COMMANDS.put(CommandType.ADD_BUSINESS_ASSOCIATE, new AddNewBusinessAssociateCommand());
        COMMANDS.put(CommandType.CHANGE_SPENDING_LIMIT, new ChangeSpendingLimitCommand());
        COMMANDS.put(CommandType.CHANGE_DEPOSIT_LIMIT, new ChangeDepositLimitCommand());
        COMMANDS.put(CommandType.DEFAULT, new DefaultCommand());
    }

    /**
     * Get the command based on the type
     * @param type the type of the command
     * @return the instance of te current command
     */
    public static Command getCommand(final CommandType type) {
        return COMMANDS.getOrDefault(type, new DefaultCommand());
    }
}
