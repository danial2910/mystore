package com.store.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class BillingCalculatorTest {

    private BillingCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new BillingCalculator();
        ReflectionTestUtils.setField(calculator, "ratePerHour", 5);
        ReflectionTestUtils.setField(calculator, "lateFeeMultiplier", 1.5);
    }

    @Test
    void calculate_noStorageEndTime_chargesNormalRate() {
        LocalDateTime start = LocalDateTime.now().minusHours(3);
        LocalDateTime end = LocalDateTime.now();

        BillingResult result = calculator.calculate(start, end, null);

        assertThat(result.getHours()).isEqualTo(3);
        assertThat(result.getLateHours()).isZero();
        assertThat(result.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(15));
        assertThat(result.isOverdue()).isFalse();
    }

    @Test
    void calculate_underOneHour_chargesMinimumOneHour() {
        LocalDateTime start = LocalDateTime.now().minusMinutes(30);
        LocalDateTime end = LocalDateTime.now();

        BillingResult result = calculator.calculate(start, end, null);

        assertThat(result.getHours()).isEqualTo(1);
        assertThat(result.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(5));
        assertThat(result.isOverdue()).isFalse();
    }

    @Test
    void calculate_endBeforeStorageEndTime_notOverdue() {
        LocalDateTime start = LocalDateTime.now().minusHours(2);
        LocalDateTime end = LocalDateTime.now();
        LocalDateTime storageEndTime = end.plusHours(4);

        BillingResult result = calculator.calculate(start, end, storageEndTime);

        assertThat(result.getHours()).isEqualTo(2);
        assertThat(result.getLateHours()).isZero();
        assertThat(result.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(10));
        assertThat(result.isOverdue()).isFalse();
    }

    @Test
    void calculate_partiallyOverdue_splitsNormalAndLateHours() {
        LocalDateTime start = LocalDateTime.now().minusHours(5);
        LocalDateTime storageEndTime = LocalDateTime.now().minusHours(2);
        LocalDateTime end = LocalDateTime.now();

        BillingResult result = calculator.calculate(start, end, storageEndTime);

        // 3 normal hours (start -> storageEndTime) + 2 late hours (storageEndTime -> end)
        assertThat(result.getHours()).isEqualTo(5);
        assertThat(result.getLateHours()).isEqualTo(2);
        // 3 * 5 + 2 * 5 * 1.5 = 15 + 15 = 30
        assertThat(result.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(30));
        assertThat(result.isOverdue()).isTrue();
    }

    @Test
    void calculate_fullyOverdue_allHoursAtLateRate() {
        LocalDateTime storageEndTime = LocalDateTime.now().minusHours(4);
        LocalDateTime start = storageEndTime.plusHours(1);
        LocalDateTime end = LocalDateTime.now();

        BillingResult result = calculator.calculate(start, end, storageEndTime);

        assertThat(result.getLateHours()).isEqualTo(result.getHours());
        // 3 hours * 5 * 1.5 = 22.5
        assertThat(result.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(22.5));
        assertThat(result.isOverdue()).isTrue();
    }

    @Test
    void calculate_overdueByLessThanOneHour_chargesMinimumOneLateHour() {
        LocalDateTime storageEndTime = LocalDateTime.now().minusMinutes(30);
        LocalDateTime start = LocalDateTime.now().minusHours(3);
        LocalDateTime end = LocalDateTime.now();

        BillingResult result = calculator.calculate(start, end, storageEndTime);

        assertThat(result.getLateHours()).isEqualTo(1);
        assertThat(result.isOverdue()).isTrue();
    }
}
