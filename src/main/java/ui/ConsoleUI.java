package ui;

import exceptions.AuthException;
import exceptions.NotFoundException;
import exceptions.ValidationException;
import model.*;
import service.FinanceService;
import storage.DataStore;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Scanner;

public class ConsoleUI {

    private final Scanner sc = new Scanner(System.in);
    private final FinanceService service;
    private User current;

    public ConsoleUI() {
        DataStore store = new DataStore();
        store.load();
        this.service = new FinanceService(store);
    }

    public void run() {
        auth();
        mainMenu();
    }

    // ==========================
    //        АВТОРИЗАЦИЯ
    // ==========================
    private void auth() {
        System.out.println("=== Вход ===");

        while (true) {
            String login = InputValidator.readNonEmptyString(sc, "Логин: ");
            String pass = InputValidator.readNonEmptyString(sc, "Пароль: ");

            try {
                current = service.login(login, pass);
                System.out.println("Добро пожаловать, " + current.getLogin());
                return;
            } catch (AuthException e) {
                System.out.println("Ошибка: " + e.getMessage());
            }
        }
    }

    // ==========================
    //        ГЛАВНОЕ МЕНЮ
    // ==========================
    private void mainMenu() {
        while (true) {
            System.out.println("\n=== Главное меню ===");
            System.out.println("1. Кошелёк");
            System.out.println("2. Бюджет");
            System.out.println("3. Операции");
            System.out.println("4. Выгрузить отчёт");
            System.out.println("0. Выход");

            switch (readInt()) {
                case 1 -> walletMenu();
                case 2 -> budgetMenu();
                case 3 -> operationsMenu();
                case 4 -> exportReport();
                case 0 -> { service.save(); return; }
            }
        }
    }

    // ==========================
    //          КОШЕЛЁК
    // ==========================
    private void walletMenu() {
        System.out.println("\n=== Кошелёк ===");
        System.out.println("Баланс: " + current.getBalance());
        System.out.println("1. Перевод");
        System.out.println("0. Назад");

        if (readInt() == 1) makeTransfer();
    }

    private void makeTransfer() {
        String to = InputValidator.readNonEmptyString(sc, "Кому (логин): ");
        double amount = InputValidator.readPositiveDouble(sc, "Сумма: ");

        try {
            service.transfer(current, to, amount);
            System.out.println("Перевод выполнен");
        } catch (NotFoundException | ValidationException e) {
            System.out.println("Ошибка: " + e.getMessage());
        }
    }

    // ==========================
    //           БЮДЖЕТ
    // ==========================
    private void budgetMenu() {
        System.out.println("\n=== Бюджет ===");
        System.out.println("1. Добавить");
        System.out.println("2. Изменить");
        System.out.println("3. Просмотр фактических");
        System.out.println("0. Назад");

        switch (readInt()) {
            case 1 -> addBudget();
            case 2 -> editBudget();
            case 3 -> viewActuals();
        }
    }

    private void addBudget() {
        OperationType type = InputValidator.readOperationType(sc, "Тип");
        String sub = InputValidator.readNonEmptyString(sc, "Подтип: ");
        YearMonth ym = InputValidator.readYearMonth(sc, "Месяц");
        double limit = InputValidator.readPositiveDouble(sc, "Лимит: ");

        try {
            service.addBudget(current, type, sub, ym, limit);
            System.out.println("Бюджет добавлен");
        } catch (ValidationException e) {
            System.out.println("Ошибка: " + e.getMessage());
        }
    }

    private void editBudget() {
        String sub = InputValidator.readNonEmptyString(sc, "Подтип: ");
        YearMonth ym = InputValidator.readYearMonth(sc, "Месяц");
        double limit = InputValidator.readPositiveDouble(sc, "Новый лимит: ");

        try {
            service.editBudget(current, sub, ym, limit);
            System.out.println("Лимит обновлён");
        } catch (NotFoundException | ValidationException e) {
            System.out.println("Ошибка: " + e.getMessage());
        }
    }

    private void viewActuals() {
        YearMonth ym = InputValidator.readYearMonth(sc, "Месяц");

        System.out.println("\n=== Доходы ===");
        double totalIncome = printActualsForType(OperationType.INCOME, ym);

        System.out.println("\n=== Расходы ===");
        double totalExpense = printActualsForType(OperationType.EXPENSE, ym);

        System.out.println("\n=== ИТОГИ ЗА " + ym + " ===");
        System.out.println("Общий доход:   " + totalIncome);
        System.out.println("Общий расход:  " + totalExpense);
        System.out.println("Чистый итог:   " + (totalIncome - totalExpense));
    }


    private double printActualsForType(OperationType type, YearMonth ym) {
        double totalFact = 0;

        for (BudgetItem b : current.getBudgets()) {
            if (b.getType() == type && b.getMonth().equals(ym)) {

                double fact = (type == OperationType.INCOME)
                        ? service.getIncomeByMonthAndSubType(current, b.getSubType(), ym)
                        : service.getSpentByMonthAndSubType(current, b.getSubType(), ym);

                double limit = b.getLimit();
                double remainder = limit - fact;

                totalFact += fact;

                System.out.println("\nКатегория: " + b.getSubType());
                System.out.println("  Лимит:     " + limit);
                System.out.println("  Факт:      " + fact);
                System.out.println("  Остаток:   " + remainder);
            }
        }

        return totalFact;
    }




    // ==========================
    //          ОПЕРАЦИИ
    // ==========================
    private void operationsMenu() {
        System.out.println("\n=== Операции ===");
        System.out.println("1. Доход");
        System.out.println("2. Расход");
        System.out.println("3. Просмотр");
        System.out.println("0. Назад");

        switch (readInt()) {
            case 1 -> addOperation(OperationType.INCOME);
            case 2 -> addOperation(OperationType.EXPENSE);
            case 3 -> listOperations();
        }
    }

    private void addOperation(OperationType type) {
        String sub = InputValidator.readNonEmptyString(sc, "Подтип: ");
        double amount = InputValidator.readPositiveDouble(sc, "Сумма: ");
        String name = InputValidator.readNonEmptyString(sc, "Название: ");

        try {
            service.addOperation(current, new Operation(type, sub, amount, name, LocalDate.now()));
            System.out.println("Операция добавлена");
        } catch (ValidationException e) {
            System.out.println("Ошибка: " + e.getMessage());
        }
    }

    private void listOperations() {
        List<Operation> ops = current.getOperations();
        if (ops.isEmpty()) {
            System.out.println("Нет операций");
            return;
        }
        ops.forEach(o -> System.out.println("  " + o));
    }

    // ==========================
    //        ОТЧЁТ
    // ==========================
    private void exportReport() {
        String path = InputValidator.readNonEmptyString(sc, "Введите название в формате NAME.csv: ");

        try {
            service.exportFullReport(current, path);
            System.out.println("Отчёт выгружен в папку проекта");
        } catch (ValidationException e) {
            System.out.println("Ошибка: " + e.getMessage());
        }
    }

    // ==========================
    //     ВСПОМОГАТЕЛЬНОЕ
    // ==========================
    private int readInt() {
        while (true) {
            try {
                return Integer.parseInt(sc.nextLine());
            } catch (Exception e) {
                System.out.print("> ");
            }
        }
    }
}
