package service;

import exceptions.AuthException;
import exceptions.NotFoundException;
import exceptions.ValidationException;
import model.*;
import storage.DataStore;


import java.io.FileWriter;
import java.io.IOException;
import java.time.YearMonth;
import java.util.*;

public class FinanceService {

    private final DataStore store;

    public FinanceService(DataStore store) {
        this.store = store;
    }

    // ==========================
    //          АВТОРИЗАЦИЯ
    // ==========================
    public User login(String login, String pass) throws AuthException {
        User user = store.getUser(login);

        if (user == null) {
            user = new User(login, pass);
            store.addUser(user);
            store.save();
            return user;
        }

        if (!user.getPasswordHash().equals(pass)) {
            throw new AuthException("Неверный пароль");
        }

        return user;
    }

    // ==========================
    //           БЮДЖЕТ
    // ==========================
    public void addBudget(User u, OperationType type, String subType, YearMonth month, double limit)
            throws ValidationException {

        if (limit <= 0)
            throw new ValidationException("Лимит должен быть больше нуля");

        if (subType == null || subType.isBlank())
            throw new ValidationException("Подтип не может быть пустым");

        u.addBudget(new BudgetItem(type, subType, month, limit));
        store.save();
    }

    public List<BudgetItem> getBudgets(User u) {
        return u.getBudgets();
    }

    public void editBudget(User u, String subType, YearMonth month, double newLimit)
            throws NotFoundException, ValidationException {

        if (newLimit <= 0)
            throw new ValidationException("Лимит должен быть больше нуля");

        for (BudgetItem b : u.getBudgets()) {
            if (b.getSubType().equalsIgnoreCase(subType) && b.getMonth().equals(month)) {
                b.setLimit(newLimit);
                store.save();
                return;
            }
        }

        throw new NotFoundException("Бюджет не найден");
    }

    // ==========================
    //          ОПЕРАЦИИ
    // ==========================
    public void addOperation(User u, Operation op) throws ValidationException {
        if (op.getAmount() <= 0)
            throw new ValidationException("Сумма должна быть больше нуля");

        u.addOperation(op);
        store.save();
        checkLimit(u, op);
    }

    private void checkLimit(User u, Operation op) {
        if (op.getType() != OperationType.EXPENSE) return;

        YearMonth ym = YearMonth.from(op.getDate());

        Optional<BudgetItem> budget = u.getBudgets().stream()
                .filter(b -> b.getMonth().equals(ym)
                        && b.getSubType().equalsIgnoreCase(op.getSubType())
                        && b.getType() == OperationType.EXPENSE)
                .findFirst();

        if (budget.isPresent()) {
            double limit = budget.get().getLimit();
            double spent = getSpentByMonthAndSubType(u, op.getSubType(), ym);
            double percent = spent / limit * 100;

            if (percent >= 100) {
                System.out.println("⚠ Превышен лимит по категории " + op.getSubType() + " за " + ym + "!");
            } else if (percent >= 80) {
                System.out.println("⚠ Достигнуто 80% лимита по категории " + op.getSubType() + ".");
            }
        }
    }

    public double getSpentByMonthAndSubType(User u, String subType, YearMonth ym) {
        return u.getOperations().stream()
                .filter(o -> o.getType() == OperationType.EXPENSE
                        && o.getSubType().equalsIgnoreCase(subType)
                        && YearMonth.from(o.getDate()).equals(ym))
                .mapToDouble(Operation::getAmount)
                .sum();
    }

    public double getIncomeByMonthAndSubType(User u, String subType, YearMonth ym) {
        return u.getOperations().stream()
                .filter(o -> o.getType() == OperationType.INCOME
                        && o.getSubType().equalsIgnoreCase(subType)
                        && YearMonth.from(o.getDate()).equals(ym))
                .mapToDouble(Operation::getAmount)
                .sum();
    }

    public Map<String, Double> getActualDifference(User u, YearMonth ym, OperationType type) {
        Map<String, Double> diff = new HashMap<>();

        for (BudgetItem b : u.getBudgets()) {
            if (b.getMonth().equals(ym) && b.getType() == type) {
                double actual = (type == OperationType.INCOME)
                        ? getIncomeByMonthAndSubType(u, b.getSubType(), ym)
                        : getSpentByMonthAndSubType(u, b.getSubType(), ym);

                diff.put(b.getSubType(), b.getLimit() - actual);
            }
        }

        return diff;
    }

    // ==========================
    //          ПЕРЕВОД
    // ==========================
    public void transfer(User from, String to, double amount)
            throws NotFoundException, ValidationException {

        if (amount <= 0)
            throw new ValidationException("Сумма перевода должна быть больше нуля");

        if (amount > from.getBalance())
            throw new ValidationException("Недостатчно средств на кошельке");

        User recipient = store.getUser(to);

        if (recipient == null)
            throw new NotFoundException("Получатель не найден");

        addOperation(from, new Operation(
                OperationType.EXPENSE,
                "перевод " + to,
                amount,
                "Перевод",
                java.time.LocalDate.now()
        ));

        addOperation(recipient, new Operation(
                OperationType.INCOME,
                "перевод от " + from.getLogin(),
                amount,
                "Перевод",
                java.time.LocalDate.now()
        ));
    }

    // ==========================
    //          ОТЧЁТ
    // ==========================
    public void exportFullReport(User user, String filePath) throws ValidationException {
        try (FileWriter fw = new FileWriter(filePath)) {

            fw.write("=== КОШЕЛЁК ===\n");
            fw.write("Пользователь: " + user.getLogin() + "\n");
            fw.write("Текущий баланс: " + user.getBalance() + " у.е.\n\n");

            fw.write("=== БЮДЖЕТ ===\n");
            fw.write("Тип,Подтип,Месяц,Установленный лимит,Фактическая сумма,Разница\n");

            for (BudgetItem b : user.getBudgets()) {
                double fact = (b.getType() == OperationType.EXPENSE)
                        ? getSpentByMonthAndSubType(user, b.getSubType(), b.getMonth())
                        : getIncomeByMonthAndSubType(user, b.getSubType(), b.getMonth());

                double diff = b.getLimit() - fact;

                fw.write(String.join(",",
                        b.getType().name(),
                        b.getSubType(),
                        b.getMonth().toString(),
                        String.valueOf(b.getLimit()),
                        String.valueOf(fact),
                        String.valueOf(diff)) + "\n");
            }

            fw.write("\n=== ОПЕРАЦИИ ===\n");
            fw.write("Дата,Тип,Подтип,Сумма,Название\n");

            for (Operation o : user.getOperations()) {
                fw.write(String.join(",",
                        o.getDate().toString(),
                        o.getType().name(),
                        o.getSubType(),
                        String.valueOf(o.getAmount()),
                        o.getName()) + "\n");
            }

            fw.write("\n=== КОНЕЦ ОТЧЁТА ===\n");

        } catch (IOException e) {
            throw new ValidationException("Не удалось записать файл: " + e.getMessage());
        }
    }

    public void save() {
        store.save();
    }
}
