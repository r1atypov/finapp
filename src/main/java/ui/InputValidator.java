package ui;

import model.OperationType;

import java.time.YearMonth;
import java.util.Scanner;

public class InputValidator {

    public static String readNonEmptyString(Scanner sc, String message) {
        while (true) {
            System.out.print(message);
            String input = sc.nextLine().trim();
            if (!input.isEmpty()) return input;
            System.out.println("Строка не может быть пустой. Попробуйте снова.");
        }
    }

    public static double readPositiveDouble(Scanner sc, String message) {
        while (true) {
            System.out.print(message);
            String input = sc.nextLine().trim();
            try {
                double value = Double.parseDouble(input);
                if (value <= 0) {
                    System.out.println("Введите число больше нуля.");
                    continue;
                }
                return value;
            } catch (NumberFormatException e) {
                System.out.println("Некорректное число. Пример: 123.45");
            }
        }
    }

    public static YearMonth readYearMonth(Scanner sc, String message) {
        while (true) {
            System.out.print(message + " (формат YYYY-MM): ");
            String input = sc.nextLine().trim();
            try {
                return YearMonth.parse(input);
            } catch (Exception e) {
                System.out.println("Неверный формат. Пример: 2024-12");
            }
        }
    }

    public static OperationType readOperationType(Scanner sc, String message) {
        while (true) {
            System.out.print(message + " (INCOME или EXPENSE): ");
            String input = sc.nextLine().trim().toUpperCase();
            try {
                return OperationType.valueOf(input);
            } catch (IllegalArgumentException e) {
                System.out.println("Допустимые значения: INCOME или EXPENSE.");
            }
        }
    }

    public static boolean readYesNo(Scanner sc, String message) {
        while (true) {
            System.out.print(message + " (y/n): ");
            String input = sc.nextLine().trim().toLowerCase();
            if (input.equals("y") || input.equals("yes")) return true;
            if (input.equals("n") || input.equals("no")) return false;
            System.out.println("Введите 'y' или 'n'.");
        }
    }
}

