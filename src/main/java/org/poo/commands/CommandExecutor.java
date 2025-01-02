package org.poo.commands;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.poo.fileio.CommandInput;
import org.poo.models.User;
import org.poo.services.Commerciant;
import org.poo.services.CurrencyConverter;

import java.util.List;

/**
 * Class that executes the commands.
 */
public class CommandExecutor {
    private final CommandContext context;

    public CommandExecutor(final List<User> users,
                           final List<Commerciant> commerciants,
                           final ObjectMapper objectMapper,
                           final ArrayNode output,
                           final CurrencyConverter currencyConverter) {
        this.context = new CommandContext(users, commerciants, objectMapper, output, currencyConverter);
    }

    /**
     * Executes the command.
     * @param command the command to be executed.
     */
    public void execute(final CommandInput command) throws Exception {
        CommandType commandType = CommandType.fromCommandName(command.getCommand());
        CommandActions actions = CommandActions.getInstance();

        switch (commandType) {
            case PRINT_USERS -> actions.printUsers(command, context);
            case ADD_ACCOUNT -> actions.addAccount(command, context);
            case CREATE_CARD -> actions.createCard(command, context);
            case CREATE_ONE_TIME_CARD -> actions.createOneTimeCard(command, context);
            case ADD_FUNDS -> actions.addFunds(command, context);
            case DELETE_ACCOUNT -> actions.deleteAccount(command, context);
            case DELETE_CARD -> actions.deleteCard(command, context);
            case PAY_ONLINE -> actions.payOnline(command, context);
            case SEND_MONEY -> actions.sendMoney(command, context);
            case SET_ALIAS -> actions.makeAnAlias(command, context);
            case PRINT_TRANSACTIONS -> actions.history(command, context);
            case SET_MINIMUM_BALANCE -> actions.setMinBalance(command, context);
            case CHECK_CARD_STATUS -> actions.checkCardStatus(command, context);
            case SPLIT_PAYMENT -> actions.splitPayment(command, context);
            case REPORT -> actions.report(command, context);
            case SPENDINGS_REPORT -> actions.spendingsReport(command, context);
            case ADD_INTEREST -> actions.addInterest(command, context);
            case CHANGE_INTEREST_RATE -> actions.changeInterestRate(command, context);
            case WITHDRAW_SAVINGS -> actions.withdrawSavings(command, context);
            case UPGRADE_PLAN -> actions.upgradePlan(command, context);
            case CASH_WITHDRAWAL -> actions.cashWithdrawal(command, context);
            default -> actions.handleDefault(command, context);
        }
    }
}


