package model;

import java.io.Serializable;
import java.util.*;

public class User implements Serializable {
    private String login;
    private String passwordHash;
    private final List<Operation> operations = new ArrayList<>();
    private final List<BudgetItem> budgets = new ArrayList<>();

    public User(String login, String passwordHash) {
        this.login = login;
        this.passwordHash = passwordHash;
    }

    public String getLogin() { return login; }
    public String getPasswordHash() { return passwordHash; }
    public List<Operation> getOperations() { return operations; }
    public List<BudgetItem> getBudgets() { return budgets; }

    public void addOperation(Operation o) { operations.add(o); }
    public void addBudget(BudgetItem b) { budgets.add(b); }

    public double getBalance() {
        double income = operations.stream()
                .filter(o -> o.getType() == OperationType.INCOME)
                .mapToDouble(Operation::getAmount).sum();
        double expense = operations.stream()
                .filter(o -> o.getType() == OperationType.EXPENSE)
                .mapToDouble(Operation::getAmount).sum();
        return income - expense;
    }

    public Set<String> getAllSubTypes() {
        Set<String> subs = new HashSet<>();
        for (BudgetItem b : budgets) subs.add(b.getSubType());
        for (Operation o : operations) subs.add(o.getSubType());
        return subs;
    }
}