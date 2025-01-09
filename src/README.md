

Pentru a îmbunătăți arhitectura proiectului și a respecta mai bine principiile OOP 
(precum *Single Responsibility Principle* și *Open/Closed Principle*), 
implementarea unui design pattern **Command** este o alegere excelentă. 
Acesta îmi permite să separ logica fiecărei comenzi într-o clasă separată,
reducând dimensiunea și complexitatea clasei `CommandActions` (din etapa trecuta).

### Implementarea pattern-ul Command

1. **Creez o interfață `Command`**:
   Interfața va defini metoda comună pentru toate comenzile, cum ar fi `execute`.

2. **Creez o clasă pentru fiecare comandă**:
   Fiecare comandă din `CommandType` ar trebui să aibă propria clasă care implementează interfața `Command` și conține logica specifică.

3. **Actualizez `CommandExecutor`**:
   În loc să folosească un `switch`, `CommandExecutor` va utiliza o mapă (sau alt mecanism) pentru a mapa fiecare tip de comandă la o clasă specifică.

4. **Simplific `CommandActions`**:
   Această clasă este eliminată complet.

### 1. **Interfața `Command`**
```java
public interface Command {
    void execute(CommandInput command, CommandContext context) throws Exception;
}
```
### 2. **Clasele specifice pentru comenzi**
Fiecare comandă va avea propria clasă.

### 3. **Registrul comenzilor**
Creez un registru care mapează `CommandType` la clasele specifice.

### 4. **Actualizarea clasei `CommandExecutor`**
Modific metoda `execute` astfel încât să utilizeze registrul.

### Avantajele acestui design
1. **Respectarea SRP**: Fiecare clasă se ocupă de o singură comandă.
2. **Extensibilitate**: Adăugarea unei noi comenzi implică doar crearea unei noi clase și adăugarea ei în registru, fără a modifica alte clase.
3. **Testabilitate**: Poți testa fiecare comandă individual.
4. **Reducerea complexității**: Clasa `CommandActions` nu mai este necesară.

### Implementarea strategiilor de cashback pentru comercianți
1. **Modularitate:** Utilizarea unei interfețe (`CashbackStrategy`) permite adăugarea de noi metode de calcul a rambursării fără a modifica clasele existente, aderând la Principiul Deschis/Închis.
2. **Flexibilitate:** Clasa `Comerciant` poate schimba dinamic strategiile de rambursare în funcție de configurația sa, susținând diverse politici ale comercianților.
3. **Personalizabilitate:** Strategii precum `NrOfTransactionsCashback` și `SpendingThresholdCashback` se adresează unor mecanisme specifice de recompensă, oferind comercianților stimulente flexibile pentru clienții lor.
4. **Mantenibilitate:** Înregistrarea și separarea clară a preocupărilor simplifică depanarea și îmbunătățirile viitoare.