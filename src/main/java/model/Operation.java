package model;

import java.io.Serializable;
import java.time.LocalDate;

public class Operation implements Serializable {
    private final OperationType type;
    private final String subType;
    private final double amount;
    private final String name;
    private final LocalDate date;

    public Operation(OperationType type, String subType, double amount, String name, LocalDate date) {
        this.type = type;
        this.subType = subType;
        this.amount = amount;
        this.name = name;
        this.date = date;
    }

    public OperationType getType() { return type; }
    public String getSubType() { return subType; }
    public double getAmount() { return amount; }
    public String getName() { return name; }
    public LocalDate getDate() { return date; }

    @Override
    public String toString() {
        return date + " | " + type + " | " + subType + " | " + amount + " | " + name;
    }
}