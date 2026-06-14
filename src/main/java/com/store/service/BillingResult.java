package com.store.service;

import java.math.BigDecimal;

public class BillingResult {

    private final long hours;
    private final long lateHours;
    private final BigDecimal amount;
    private final boolean overdue;

    public BillingResult(long hours, long lateHours, BigDecimal amount, boolean overdue) {
        this.hours = hours;
        this.lateHours = lateHours;
        this.amount = amount;
        this.overdue = overdue;
    }

    public long getHours() {
        return hours;
    }

    public long getLateHours() {
        return lateHours;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public boolean isOverdue() {
        return overdue;
    }
}
