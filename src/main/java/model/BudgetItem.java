package model;

import java.io.Serializable;
import java.time.YearMonth;

public class BudgetItem implements Serializable {
    private final OperationType type;
    private final String subType;
    private final YearMonth month;
    private double limit;

    public BudgetItem(OperationType type, String subType, YearMonth month, double limit) {
        this.type = type;
        this.subType = subType;
        this.month = month;
        this.limit = limit;
    }

    public OperationType getType() { return type; }
    public String getSubType() { return subType; }
    public YearMonth getMonth() { return month; }
    public double getLimit() { return limit; }
    public void setLimit(double limit) { this.limit = limit; }

    @Override
    public String toString() {
        return type + " | " + subType + " | " + month + " | лимит: " + limit;
    }
}