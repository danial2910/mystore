package com.store.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class BillingCalculator {

    @Value("${app.rental.rate-per-hour}")
    private int ratePerHour;

    @Value("${app.rental.late-fee-multiplier}")
    private double lateFeeMultiplier;

    public BillingResult calculate(LocalDateTime start, LocalDateTime end, LocalDateTime storageEndTime) {
        boolean overdue = storageEndTime != null && end.isAfter(storageEndTime);

        long normalHours;
        long lateHours;

        if (overdue) {
            LocalDateTime lateStart = start.isAfter(storageEndTime) ? start : storageEndTime;
            normalHours = Math.max(0, ChronoUnit.HOURS.between(start, storageEndTime));
            lateHours = Math.max(1, ChronoUnit.HOURS.between(lateStart, end));
        } else {
            normalHours = Math.max(1, ChronoUnit.HOURS.between(start, end));
            lateHours = 0;
        }

        BigDecimal amount = BigDecimal.valueOf((long) ratePerHour * normalHours);

        if (lateHours > 0) {
            BigDecimal lateAmount = BigDecimal.valueOf(ratePerHour)
                    .multiply(BigDecimal.valueOf(lateFeeMultiplier))
                    .multiply(BigDecimal.valueOf(lateHours));
            amount = amount.add(lateAmount);
        }

        return new BillingResult(normalHours + lateHours, lateHours, amount, overdue);
    }

    public double getLateFeeMultiplier() {
        return lateFeeMultiplier;
    }
}
