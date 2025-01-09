package org.poo.commands;

import java.util.HashMap;
import java.util.Map;

public class CommandRegistry {
    private static final Map<CommandType, Command> commands = new HashMap<>();

    static {
        commands.put(CommandType.PRINT_USERS, new PrintUsersCommand());
        commands.put(CommandType.ADD_ACCOUNT, new AddAccountCommand());
        commands.put(CommandType.CREATE_CARD, new CreateCardCommand());
        commands.put(CommandType.CREATE_ONE_TIME_CARD, new CreateOneTimeCardCommand());
        commands.put(CommandType.ADD_FUNDS, new AddFundsCommand());
        commands.put(CommandType.DELETE_ACCOUNT, new DeleteAccountCommand());
        commands.put(CommandType.DELETE_CARD, new DeleteCardCommand());
        commands.put(CommandType.PAY_ONLINE, new PayOnlineCommand());
        commands.put(CommandType.SEND_MONEY, new SendMoneyCommand());
        commands.put(CommandType.SET_ALIAS, new SetAliasCommand());
        commands.put(CommandType.PRINT_TRANSACTIONS, new PrintTransactionsCommand());
        commands.put(CommandType.SET_MINIMUM_BALANCE, new SetMinimumBalanceCommand());
        commands.put(CommandType.CHECK_CARD_STATUS, new CheckCardStatusCommand());
        commands.put(CommandType.SPLIT_PAYMENT, new SplitPaymentCommand());
        commands.put(CommandType.REPORT, new ReportCommand());
        commands.put(CommandType.SPENDINGS_REPORT, new SpendingsReportCommand());
        commands.put(CommandType.BUSINESS_REPORT, new BusinessReportCommand());
        commands.put(CommandType.ADD_INTEREST, new AddInterestCommand());
        commands.put(CommandType.CHANGE_INTEREST_RATE, new ChangeInterestRateCommand());
        commands.put(CommandType.WITHDRAW_SAVINGS, new WithdrawSavingsCommand());
        commands.put(CommandType.UPGRADE_PLAN, new UpgradePlanCommand());
        commands.put(CommandType.CASH_WITHDRAWAL, new CashWithdrawalCommand());
        commands.put(CommandType.ACCEPT_SPLIT_PAYMENT, new AcceptSplitPaymentCommand());
        commands.put(CommandType.REJECT_SPLIT_PAYMENT, new RejectSplitPaymentCommand());
        commands.put(CommandType.ADD_BUSINESS_ASSOCIATE, new AddNewBusinessAssociateCommand());
        commands.put(CommandType.CHANGE_SPENDING_LIMIT, new ChangeSpendingLimitCommand());
        commands.put(CommandType.CHANGE_DEPOSIT_LIMIT, new ChangeDepositLimitCommand());
        commands.put(CommandType.DEFAULT, new DefaultCommand());
    }

    public static Command getCommand(CommandType type) {
        return commands.getOrDefault(type, new DefaultCommand());
    }
}
