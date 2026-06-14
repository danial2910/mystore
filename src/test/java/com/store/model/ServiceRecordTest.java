package com.store.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

class ServiceRecordTest {

    @Test
    void settersAndGetters_roundTrip() {
        Item item = new Item();
        User owner = new User();
        LocalDateTime start = LocalDateTime.of(2026, 1, 1, 8, 0);
        LocalDateTime end = LocalDateTime.of(2026, 1, 1, 11, 0);

        ServiceRecord record = new ServiceRecord();
        record.setId(5);
        record.setItem(item);
        record.setOwner(owner);
        record.setStartTime(start);
        record.setEndTime(end);
        record.setTotalHours(3);
        record.setTotalAmount(BigDecimal.valueOf(15));
        record.setStatus(ServiceRecordStatus.ENDED);
        record.setPaid(true);
        record.setPaidAt(end);

        assertThat(record.getId()).isEqualTo(5);
        assertThat(record.getItem()).isEqualTo(item);
        assertThat(record.getOwner()).isEqualTo(owner);
        assertThat(record.getStartTime()).isEqualTo(start);
        assertThat(record.getEndTime()).isEqualTo(end);
        assertThat(record.getTotalHours()).isEqualTo(3);
        assertThat(record.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(15));
        assertThat(record.getStatus()).isEqualTo(ServiceRecordStatus.ENDED);
        assertThat(record.isPaid()).isTrue();
        assertThat(record.getPaidAt()).isEqualTo(end);
    }

    @Test
    void defaultStatus_isActive() {
        ServiceRecord record = new ServiceRecord();
        assertThat(record.getStatus()).isEqualTo(ServiceRecordStatus.ACTIVE);
    }

    @Test
    void defaultPaid_isFalse() {
        ServiceRecord record = new ServiceRecord();
        assertThat(record.isPaid()).isFalse();
        assertThat(record.getPaidAt()).isNull();
    }
}
