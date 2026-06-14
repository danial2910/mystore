package com.store.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.Date;

import org.junit.jupiter.api.Test;

class ItemTest {

    @Test
    void settersAndGetters_roundTrip() {
        User owner = new User();
        owner.setUsername("owner");

        User admin = new User();
        admin.setUsername("admin");

        LocalDateTime start = LocalDateTime.of(2026, 1, 1, 10, 0);
        LocalDateTime end = LocalDateTime.of(2026, 1, 1, 16, 0);
        LocalDateTime requestedEnd = LocalDateTime.of(2026, 1, 2, 16, 0);
        LocalDateTime approvedAt = LocalDateTime.of(2026, 1, 1, 9, 0);
        LocalDateTime rejectedAt = LocalDateTime.of(2026, 1, 1, 9, 30);
        Date createdAt = new Date();

        Item item = new Item();
        item.setId(1);
        item.setName("Box");
        item.setBrand("Acme");
        item.setCategory("electronics");
        item.setQuantity(3);
        item.setDescription("A test item description.");
        item.setImageFileName("box.jpg");
        item.setCreatedAt(createdAt);
        item.setStatus(ItemStatus.APPROVED);
        item.setStorageStartTime(start);
        item.setStorageEndTime(end);
        item.setRequestedStorageEndTime(requestedEnd);
        item.setOwner(owner);
        item.setApprovedAt(approvedAt);
        item.setRejectedAt(rejectedAt);
        item.setApprovedBy(admin);

        assertThat(item.getId()).isEqualTo(1);
        assertThat(item.getName()).isEqualTo("Box");
        assertThat(item.getBrand()).isEqualTo("Acme");
        assertThat(item.getCategory()).isEqualTo("electronics");
        assertThat(item.getQuantity()).isEqualTo(3);
        assertThat(item.getDescription()).isEqualTo("A test item description.");
        assertThat(item.getImageFileName()).isEqualTo("box.jpg");
        assertThat(item.getCreatedAt()).isEqualTo(createdAt);
        assertThat(item.getStatus()).isEqualTo(ItemStatus.APPROVED);
        assertThat(item.getStorageStartTime()).isEqualTo(start);
        assertThat(item.getStorageEndTime()).isEqualTo(end);
        assertThat(item.getRequestedStorageEndTime()).isEqualTo(requestedEnd);
        assertThat(item.getOwner()).isEqualTo(owner);
        assertThat(item.getApprovedAt()).isEqualTo(approvedAt);
        assertThat(item.getRejectedAt()).isEqualTo(rejectedAt);
        assertThat(item.getApprovedBy()).isEqualTo(admin);
    }

    @Test
    void defaultStatus_isPending() {
        Item item = new Item();
        assertThat(item.getStatus()).isEqualTo(ItemStatus.PENDING);
    }
}
