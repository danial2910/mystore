package com.store.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ItemStatusTest {

    @Test
    void allValues_exist() {
        assertThat(ItemStatus.values()).containsExactlyInAnyOrder(
                ItemStatus.PENDING,
                ItemStatus.APPROVED,
                ItemStatus.REJECTED,
                ItemStatus.CANCELLED
        );
    }

    @Test
    void valueOf_returnsCorrectConstant() {
        assertThat(ItemStatus.valueOf("PENDING")).isEqualTo(ItemStatus.PENDING);
        assertThat(ItemStatus.valueOf("APPROVED")).isEqualTo(ItemStatus.APPROVED);
        assertThat(ItemStatus.valueOf("REJECTED")).isEqualTo(ItemStatus.REJECTED);
        assertThat(ItemStatus.valueOf("CANCELLED")).isEqualTo(ItemStatus.CANCELLED);
    }
}
