// Copyright Iliescu Miruna-Elena 2024-2025

# Mobile Banking Application Phase 1

This project implements a modular and scalable banking application.
Below is an overview of the project's structure, including its packages and their respective roles.

## Project Structure

### **Commands**
- **`CommandActions`**: Defines the various actions that can be performed through commands.
- **`CommandContext`**: Encapsulates the context for executing commands, such as user lists and external services.
- **`CommandExecutor`**: Executes commands based on the input and context provided.
- **`CommandType`**: Enumerates different types of commands supported by the application.

### **Exceptions**
Custom exceptions are used for robust error handling:
- **`AccountNotFoundException`**: Thrown when a specified account cannot be located.
- **`CardNotFoundException`**: Thrown when a specified card cannot be found.
- **`CurrencyConversionException`**: Indicates an error during currency conversion.
- **`InsufficientFundsException`**: Raised when an account has insufficient funds.
- **`UnauthorizedCardAccessException`**: Triggered when there is unauthorized access to a card.
- **`UserNotFoundException`**: Thrown when a specified user does not exist.

### **Handlers**
These classes handle specific types of banking transactions and actions:
- **`BankTransferHandler`**: Manages bank transfer operations between accounts.
- **`CardDestroyedHandler`**: Handles card destruction scenarios.
- **`CardPaymentHandler`**: Processes payments made using cards.
- **`CreateCardHandler`**: Manages the creation of new cards.
- **`DefaultTransactionHandler`**: A fallback handler for generic transactions.
- **`SplitPaymentHandler`**: Handles the splitting of payments among multiple accounts.
- **`TransactionHandler`**: Interface for transaction-related operations.

### **Main**
- **`Main`**: The entry point of the application. Responsible for initializing the context and running the program.
- **`Test`**: Contains unit tests for the application's features.

### **Models**
Core entities that represent the business logic:
- **`Account`**: Represents a bank account, including balance, IBAN, associated cards and other properties for tracking account status.
- **`Card`**: Represents a debit or credit card linked to an account.
- **`Transaction`**:  Represents a financial transaction performed by a user.
- **`TransactionExecutor`**: Executes various types of transactions using a handler-based architecture.
- **`User`**: Represents a banking user with associated accounts and transaction history.

### **Services**
Utility services that provide core functionality:
- **`CurrencyConverter`**: Handles currency conversions between different currencies.
- **`ExchangeRate`**: Represents the exchange rates used by the `CurrencyConverter`.

### **Utils**
- **`Utils`**: Contains utility methods and helper functions used across the application.

## Design Choices for Transaction Management

### **Builder Design Pattern for Transactions**
The `Transaction` class represents a financial operation performed by a user.
It utilizes the **Builder Design Pattern** to create flexible and robust transaction objects.

- **Mandatory Fields:** `timestamp`, `description`, and `account`.
   - **Optional Fields:** Other fields are customizable to accommodate various transaction types,
     such as `splitPayment`, `cardPayment`, `bankTransfer`, or `createCard`.
- **Benefits of the Builder Pattern:**
   - Simplifies the creation of complex `Transaction` objects.
   - Ensures immutability of transactions after creation.
   - Supports readability and scalability, as additional optional fields can be added without modifying the core constructor.

A transaction is constructed using the `TransactionBuilder` class, ensuring that only valid combinations of fields are included.
After construction, the transaction is processed by the `TransactionExecutor` for execution.

### **Abstract Class for Transaction Execution**
The `TransactionExecutor` class manages the execution of transactions using a **handler-based architecture**.

- **Static Handler Registry:** A `Map` associates transaction types with their respective `TransactionHandler` implementations.
- **Dynamic Dispatch:** The `executeTransaction` method dynamically invokes the appropriate handler based on the transaction type.
- **Default Behavior:** For unknown transaction types, a `DefaultTransactionHandler` is used to log errors or provide fallback logic.

This design ensures that:
1. Transaction logic remains isolated and modular.
2. New transaction types can be added easily by registering a new handler in the `TRANSACTION_HANDLERS` map.
3. Unknown transaction types are handled gracefully, improving application reliability.

### **Factory-like Interface for Transaction Handlers**
The `TransactionHandler` interface defines the contract for handling specific transaction types.
This approach allows for clean separation of concerns, where each handler focuses solely on its transaction logic.

- **Responsibilities:**
   - Encapsulates the logic for a single transaction type.
   - Processes the transaction and updates a JSON representation of the transaction details.
- **Benefits of the Interface:**
   - Promotes the **Single Responsibility Principle** by delegating transaction-specific logic to individual handlers.
   - Enhances **extensibility**, as new handlers can be added without modifying existing code.

### Example Workflow
1. **Transaction Creation:**  
   A `Transaction` object is constructed using the `TransactionBuilder` class, with mandatory and optional fields as required.

2. **Transaction Execution:**  
   The `TransactionExecutor`'s `executeTransaction` method:
   - Determines the appropriate handler for the transaction type.
   - Delegates processing to the corresponding `TransactionHandler` implementation.

3. **Transaction Handling:**  
   The handler processes the transaction, updates the `transactionJson` object with details, and ensures compliance with business rules.


This approach adheres to key software engineering principles like **Open/Closed Principle** and **Single Responsibility Principle**,
ensuring a scalable, maintainable, and flexible transaction management system.

## Design Choices for Exception Handling

Using exceptions allows error-handling logic to remain modular and consistent:
- The logic for detecting and throwing errors (e.g., checking if a user exists or if a card is active) resides in the core implementation.
- The handling of these errors is managed separately in `catch` blocks, promoting the **Single Responsibility Principle**.

### Benefits:
1. **Resilience:** Ensures the application continues running even in the face of errors.
2. **Clarity:** Isolates error-handling logic, keeping the core implementation clean and focused.
3. **Feedback:** Provides detailed error messages to users and logs all incidents for auditing.
4. **Extensibility:** New exceptions can be added easily for future requirements without disrupting existing logic.
5. **Consistency:** Guarantees that all operations—successful or failed—are logged, maintaining data integrity and traceability.

This approach aligns with best practices in software engineering by promoting **clean code**,
**robust error handling**, and **user-centric feedback mechanisms**.

## Currency Conversion Design and Usage

### **The `ExchangeRate` Class**
The `ExchangeRate` class represents a single exchange rate between two currencies.
It is designed with immutability in mind, ensuring that once an exchange rate object is created, its attributes cannot be modified.
This makes the class safe to use in a multithreaded environment and ensures data consistency.

Key attributes:
- **`from`**: The source currency.
- **`to`**: The target currency.
- **`rate`**: The exchange rate for converting the source currency to the target currency.

### **The `CurrencyConverter` Class**
The `CurrencyConverter` class is responsible for handling the logic of currency conversion in various transaction scenarios.
It operates on a list of `ExchangeRate` objects and provides a method to convert amounts between different currencies.

#### Key Features:
**Direct and Inverse Conversion**:
- The conversion algorithm supports both direct exchange rates (`from -> to`) and inverse relationships (`to -> from`)
- to handle cases where an explicit exchange rate may not exist.

**Breadth-First Search (BFS) Approach**:
- The method `convertCurrency` employs a BFS algorithm to traverse all possible conversion paths.
- This ensures that even multistep conversions (e.g., `USD -> GBP -> EUR`) are supported.

**Error Handling**:
- Throws an `IllegalArgumentException` if invalid input is provided (e.g., negative amounts or unsupported currencies).
- Throws a `CurrencyConversionException` if no valid conversion path is found.


### **Integration and Initialization in the Main Method**
In the main function, the `CurrencyConverter` is initialized with a list of exchange rates extracted from the input data.
This demonstrates the **dependency injection principle**, where the `CurrencyConverter` depends on
external data (exchange rates) provided at runtime.

## Conclusion

This mobile banking application demonstrates a **robust**, **modular**, and **scalable design** for managing transactions,
user accounts, and currency conversions. By employing well-established **software engineering principles** such as
the `Builder Design Pattern`, `Single Responsibility Principle`, and `Open/Closed Principle`, the project ensures
flexibility, maintainability, and ease of extension.


## Feedback Phase 1

The requirements for this project were much easier to understand compared to Assignment 0,
as it was based on an application that we all use in our daily lives, making it easier to
intuit what needed to be done. However, this was not enough to complete the assignment effortlessly,
as the requirements lacked details about the implementation, such as when an error should be displayed
in the output or when a transaction should return an error message.

Even though we covered more of the course material and became more familiar with this programming language and its principles,
the experience of working on this assignment was not exactly pleasant due to incorrect tests caused by inconsistencies in the requirements.


# Mobile Banking Application Phase 2

Below is an overview of the project structure, highlighting the changes
made to the implementation in the second stage.

## Changes in the project structure

### **Commands**
The following classes implement commands to handle user actions:
- **`AcceptSplitPaymentCommand`**: Allows users to accept split payments initiated by others.
- **`AddAccountCommand`**: Facilitates the creation of new bank accounts.
- **`AddFundsCommand`**: Handles adding funds to an existing account.
- **`AddInterestCommand`**: Manages the addition of interest to savings accounts.
- **`AddNewBusinessAssociateCommand`**: Adds a new business associate to the system.
- **`BusinessReportCommand`**: Generates reports on business transactions.
- **`CashWithdrawalCommand`**: Processes cash withdrawal requests from accounts.
- **`ChangeDepositLimitCommand`**: Allows modification of the deposit limit for accounts.
- **`ChangeInterestRateCommand`**: Updates the interest rate for specific accounts or plans.
- **`ChangeSpendingLimitCommand`**: Enables changes to the spending limit for users.
- **`CheckCardStatusCommand`**: Verifies the current status of a card (e.g., active, blocked).
- **`CreateCardCommand`**: Handles the creation of new cards linked to accounts.
- **`CreateOneTimeCardCommand`**: Generates single-use cards for secure transactions.
- **`DeleteAccountCommand`**: Facilitates account deletion.
- **`DeleteCardCommand`**: Handles card removal requests.
- **`PayOnlineCommand`**: Processes online payments using cards.
- **`PrintTransactionsCommand`**: Outputs a list of transactions for a specified account.
- **`PrintUsersCommand`**: Displays details of all registered users.
- **`RejectSplitPaymentCommand`**: Allows users to decline split payment requests.
- **`ReportCommand`**: Generates a detailed report of account activities.
- **`SendMoneyCommand`**: Handles fund transfers between users.
- **`SetAliasCommand`**: Assigns or updates an alias for an account.
- **`SetMinimumBalanceCommand`**: Sets a minimum balance requirement for accounts.
- **`SpendingsReportCommand`**: Summarizes spending patterns and trends.
- **`SplitPaymentCommand`**: Initiates split payments among multiple accounts.
- **`UpgradePlanCommand`**: Upgrades a user's subscription plan (e.g., from standard to gold).
- **`WithdrawSavingsCommand`**: Processes withdrawals from savings accounts.

### **Handlers**
New handlers have been added to support the newly implemented commands and features:
- **`AddFundsHandler`**: Manages adding funds to user accounts.
- **`AddInterestHandler`**: Handles the application of interest to savings accounts.
- **`BankTransferHandler`**: Facilitates bank-to-bank transfers.
- **`CardDestroyedHandler`**: Processes card destruction events.
- **`CardPaymentHandler`**: Handles card-based payment processing.
- **`CashWithdrawalHandler`**: Manages cash withdrawals from accounts.
- **`CreateCardHandler`**: Oversees the creation of cards.
- **`DefaultTransactionHandler`**: A fallback handler for undefined transactions.
- **`SplitPaymentHandler`**: Handles split payment operations among accounts and integrates
- seamlessly with the `AcceptSplitPaymentCommand` to streamline the acceptance process.
- By coordinating these components, the system ensures that split payments are initiated,
- validated, and finalized efficiently while providing real-time feedback to users.
- **`TransactionHandler`**: Abstract interface for transaction-related handlers.
- **`UpgradePlanHandler`**: Manages upgrades to user subscription plans.
- **`WithdrawalSavingsHandler`**: Processes withdrawals from savings accounts.

### **Models**
The core entities have been extended to include:
- **`GlobalResponseTracker`**: Tracks system-wide responses for analytics and debugging purposes.
- **`SplitPayment`**: Represents the splitting of payments among multiple participants.
- **`Transaction`**: Extended to support additional transaction types and detailed tracking.

### **Services**
The `services` package has been extended with new implementations:
- **`CashbackStrategy`**: Interface for calculating cashback.
- **`SpendingThresholdCashback`**: Implements cashback calculations based on user spending thresholds.
- Cashback rates vary by user plan (e.g., gold, silver, standard) and spending tiers.
- **`NrOfTransactionsCashback`**: Implements cashback calculation based on the number of transactions with a merchant.
- This service uses specific transaction thresholds and cashback rates for different merchant types:
    - **Food**: 2% cashback awarded after 3 transactions, provided the user hasn't already received cashback in this category.
    - **Clothes**: 5% cashback awarded after 5 transactions, provided the user hasn't already received cashback in this category.
    - **Tech**: 10% cashback awarded after 11 transactions, provided the user hasn't already received cashback in this category.

### **Utils**
Utility classes have been added for modularity and code reuse:
- **`CashbackRates`**: Encapsulates cashback rate constants, replacing magic numbers in the code.
- **`DebugNumbers`**: Provides utility methods for debugging numerical operations and validations.

## Design Enhancements

### Introduction of Enums for Constants
Magic numbers were removed and replaced with well-defined constants grouped into enums for better maintainability.
For instance, cashback rates used in `SpendingThresholdCashback` are now managed in `CashbackRates`.

### Improved Transaction Management
Handlers now support additional transaction types like `SplitPayment`, and the dynamic handler registry
ensures easy addition of new transaction handlers without disrupting existing logic.

### Implementing Cashback Strategies for Commerciants
1. **Modularity:** The use of an interface (`CashbackStrategy`) allows adding new methods for calculating 
the refund without modifying existing classes, adhering to the Open/Closed Principle.
2. **Flexibility:** The `Commerciant` class can dynamically change refund strategies depending on its configuration,
supporting various merchant policies.
3. **Customizability:** Strategies such as `NrOfTransactionsCashback` and `SpendingThresholdCashback`
address specific reward mechanisms, providing merchants with flexible incentives for their customers.
4. **Maintainability:** Clear logging and separation of concerns simplifies troubleshooting and future improvements.

### Error Handling Improvements
New exception types and more detailed exception messages improve application resilience and user feedback.

### Implementing the Command Pattern

To improve the project architecture and better adhere to OOP principles
(such as *Single Responsibility Principle* and *Open/Closed Principle*),
implementing a **Command** design pattern is an excellent choice.
This allows me to separate the logic of each command into a separate class,
reducing the size and complexity of the `CommandActions` class (from the previous step).

1. **Create a `Command` interface**:
   The interface will define the common method for all commands, such as `execute`.

2. **Create a class for each command**:
   Each command in `CommandType` should have its own class that implements the `Command` interface and contains the specific logic.

3. **Update `CommandExecutor`**:
   Instead of using a `switch`, `CommandExecutor` will use a map (or other mechanism) to map each command type to a specific class.

4. **Simplify `CommandActions`**:
   This class is removed completely.

#### 1. **The `Command` interface**
```java
public interface Command {
    void execute(CommandInput command, CommandContext context) throws Exception;
}
```
#### 2. **Command-specific classes**
Each command will have its own class.

#### 3. **Command registry**
I create a registry that maps `CommandType` to specific classes.

#### 4. **Update `CommandExecutor` class**
I modify the `execute` method to use the registry.

#### Advantages of this design
1. **Single Responsibility Principle**: Each class handles a single command.
2. **Extensibility**: Adding a new command simply involves creating a new class and adding it to the registry, without modifying other classes.
3. **Testability**: You can test each command individually.
4. **Complexity reduction**: The `CommandActions` class is no longer needed.

### Extending the Factory Interface for Transaction Handlers
- **`TransactionHandler`**: Abstract interface for transaction-related handlers.
- Additional handlers extending this interface have been introduced, promoting modularity and flexibility:
    - **`AddFundsHandler`**: Implements logic for handling fund additions.
    - **`BankTransferHandler`**: Handles inter-bank transfers, including validation and tracking.
    - **`UpgradePlanHandler`**: Manages upgrades to user subscription plans.
    - **`SplitPaymentHandler`**: Focuses on processing split payments among multiple accounts.

This approach ensures compliance with the **Single Responsibility Principle** and supports
the **Open/Closed Principle** by allowing the seamless addition of new handlers for other
transaction types without modifying existing code.

### Core Design Principles for the Split Payment Command (Singleton)
1. **Singleton Pattern (PaymentProcessor):**
    - The `PaymentProcessor` class employs the Singleton design pattern, ensuring that only one instance manages
    - split payments and responses throughout the application's lifecycle.
    - This approach centralizes payment management, simplifying data consistency and reducing potential race conditions.

2. **Command Pattern (Commands and Handlers):**
    - The implementation of commands like `AcceptSplitPaymentCommand`, `SplitPaymentCommand`,
   `RejectSplitPaymentCommand` follows the Command pattern.
    - Each command encapsulates a specific operation (accepting split payments or initiating them),
   promoting a clean separation of concerns and enabling easy addition of new commands without modifying the existing ones.

3. **Open/Closed Principle (OCP):**
    - The `SplitPayment` class and `PaymentProcessor` support extensibility by abstracting the logic for different payment types.
   For example, the use of `SplitPaymentType` enables the addition of new split payment mechanisms without altering core logic.

4. **Single Responsibility Principle (SRP):**
    - Each class has a clear and focused purpose:
        - `AcceptSplitPaymentCommand` handles user responses to split payments.
        - `SplitPaymentCommand` manages the initiation and setup of split payments.
        - `PaymentProcessor` centralizes the coordination and tracking of payments.
        - `GlobalResponseTracker` maintains a record of user responses to payments.
    - This design improves maintainability and testability.

#### **Scalability and Modularity**
1. **Flexible Data Structures:**
    - The use of maps in `SplitPayment` (`emailToAccount` and `accountBalances`) ensures a scalable design
   that can easily manage additional participants or payment details.

2. **Dynamic Payment Management:**
    - `PaymentProcessor` dynamically tracks and processes active split payments. This allows multiple payments to coexist,
   with each being independently managed until completion.

### Key Aspects of the Builder Design in `Transaction`

The **Builder Design Pattern** is a powerful approach used to create complex objects step-by-step,
allowing for flexibility in object construction without requiring a large number of overloaded constructors.
In the `Transaction` class, the `TransactionBuilder` provides an elegant solution for constructing transactions
with a combination of mandatory and optional fields.

1. **Separation of Concerns**:
    - The `TransactionBuilder` is a nested static class responsible for constructing instances of `Transaction`.
    - The `Transaction` class itself remains immutable and focuses solely on representing a financial transaction,
   with its fields set via the builder.

2. **Mandatory and Optional Fields**:
    - Fields such as `timestamp`, `description`, `account`, and `type` are mandatory and initialized in the `TransactionBuilder` constructor.
    - Optional fields like `senderIBAN`, `receiverIBAN`, `amount`, `error`, `cardHolder`, and more are set through
   dedicated setter methods in the builder. This ensures that the required attributes are always present,
   while optional ones can be included only when needed.

3. **Immutability**:
    - Once a `Transaction` object is built, it is immutable because all fields are declared `final`
   and are only set via the private constructor of the `Transaction` class.

4. **Extensibility**:
    - Adding new fields to `Transaction` is straightforward. For instance, if a new field `transactionCategory` is introduced,
    it can be easily incorporated into the builder by adding a new method and updating the `build` method.
    - This minimizes the risk of breaking existing code while extending the design.

### Inner Class for returning a result of a card search

The `SearchCard` inner class in the `PayOnlineCommand` class is used to store and manage the results of searching
for a user's card across multiple accounts. It helps centralize the details related to the card search,
making it easier to handle and return the results.

#### How it fits into the `PayOnlineCommand`:
- When the card isn't found in the user's own accounts, the `SearchCard` class is instantiated and passed to
the `searchUsersCard` method, which populates it with relevant data if the card is found in a business account.
This helps manage the flow of data and avoid unnecessary redefinitions of variables.

- It makes the card search logic cleaner and allows the `PayOnlineCommand` to work seamlessly even when searching through multiple accounts,
whether they're personal or business accounts, by encapsulating the result in one object.

#### Example Usage in Code:
- In the `execute` method, if the card isn't found in the user's own accounts, an instance of `SearchCard` is created:
  ```java
  SearchCard result = new SearchCard();
  searchUsersCard(command, context, cardNumber, result);
  cardUser = result.cardUser;
  accountUser = result.accountUser;
  ownerAccount = result.ownerAccount;
  ```
This makes it possible to handle the result of the card search more cleanly by encapsulating all the related information
(card, account, and owner account) in a single object that can be easily manipulated and passed through different parts of the method.

## Conclusion
Phase 2 of the Mobile Banking application builds on the robust design established in Phase 1.
By extending functionality and improving maintainability through enumerations, additional services,
transaction management improvements and introducing new designs such as Command pattern, Singleton, Factory, Strategy.
The application is more scalable and easier to extend or modify.
