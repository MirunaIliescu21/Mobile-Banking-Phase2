package org.poo.main;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.poo.checker.Checker;
import org.poo.checker.CheckerConstants;
import org.poo.commands.CommandExecutor;
import org.poo.fileio.CommandInput;
import org.poo.fileio.ExchangeInput;
import org.poo.fileio.ObjectInput;
import org.poo.fileio.UserInput;
import org.poo.models.User;
import org.poo.services.CurrencyConverter;
import org.poo.services.ExchangeRate;
import org.poo.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;

/**
 * The entry point to this homework. It runs the checker that tests your implementation.
 */
public final class Main {
    /**
     * for coding style
     */
    private Main() {
    }

    /**
     * DO NOT MODIFY MAIN METHOD
     * Call the checker
     * @param args from command line
     * @throws IOException in case of exceptions to reading / writing
     */
    public static void main(final String[] args) throws IOException {
        File directory = new File(CheckerConstants.TESTS_PATH);
        Path path = Paths.get(CheckerConstants.RESULT_PATH);

        if (Files.exists(path)) {
            File resultFile = new File(String.valueOf(path));
            for (File file : Objects.requireNonNull(resultFile.listFiles())) {
                file.delete();
            }
            resultFile.delete();
        }
        Files.createDirectories(path);

        var sortedFiles = Arrays.stream(Objects.requireNonNull(directory.listFiles())).
                sorted(Comparator.comparingInt(Main::fileConsumer))
                .toList();

        for (File file : sortedFiles) {
            String filepath = CheckerConstants.OUT_PATH + file.getName();
            File out = new File(filepath);
            boolean isCreated = out.createNewFile();
            if (isCreated) {
                action(file.getName(), filepath);
            }
        }

        Checker.calculateScore();
    }

    /**
     * @param filePath1 for input file
     * @param filePath2 for output file
     * @throws IOException in case of exceptions to reading / writing
     */
    public static void action(final String filePath1,
                              final String filePath2) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        File file = new File(CheckerConstants.TESTS_PATH + filePath1);
        ObjectInput inputData = objectMapper.readValue(file, ObjectInput.class);

        ArrayNode output = objectMapper.createArrayNode();

        // Extract the list of users from inputData
        List<User> users = new ArrayList<>();

        // Create the users (with empty accounts) from the input data
        for (UserInput userInput : inputData.getUsers()) {
            users.add(new User(userInput.getFirstName(),
                    userInput.getLastName(),
                    userInput.getEmail()));
        }

        // Extract the list of exchange rates from inputData
        List<ExchangeRate> exchangeRates = new ArrayList<>();
        if (inputData.getExchangeRates() != null) {
            for (ExchangeInput exchangeInput : inputData.getExchangeRates()) {
                exchangeRates.add(new ExchangeRate(
                        exchangeInput.getFrom(),
                        exchangeInput.getTo(),
                        exchangeInput.getRate()
                ));
            }
        }
        CurrencyConverter currencyConverter = new CurrencyConverter(exchangeRates);

        // Initialize CommandExecutor with context
        CommandExecutor commandExecutor = new CommandExecutor(users, objectMapper,
                                                              output,
                                                              currencyConverter);

        // Execute each command
        for (CommandInput command : inputData.getCommands()) {
            try {
                commandExecutor.execute(command);
            } catch (Exception e) {
                // Log or handle generic exceptions to avoid stopping execution
                System.err.println("Error executing command: " + command.getCommand());
            }
        }

        ObjectWriter objectWriter = objectMapper.writerWithDefaultPrettyPrinter();
        objectWriter.writeValue(new File(filePath2), output);

        Utils.resetRandom();
    }

    /**
     * Method used for extracting the test number from the file name.
     *
     * @param file the input file
     * @return the extracted numbers
     */
    public static int fileConsumer(final File file) {
        return Integer.parseInt(
                file.getName()
                        .replaceAll(CheckerConstants.DIGIT_REGEX, CheckerConstants.EMPTY_STR)
        );
    }
}
