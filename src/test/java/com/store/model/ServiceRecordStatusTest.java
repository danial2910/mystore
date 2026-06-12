package com.store.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ServiceRecordStatusTest {

    @Test
    void allValues_exist() {
        assertThat(ServiceRecordStatus.values()).containsExactlyInAnyOrder(
                ServiceRecordStatus.ACTIVE,
                ServiceRecordStatus.ENDED
        );
    }

    @Test
    void valueOf_returnsCorrectConstant() {
        assertThat(ServiceRecordStatus.valueOf("ACTIVE")).isEqualTo(ServiceRecordStatus.ACTIVE);
        assertThat(ServiceRecordStatus.valueOf("ENDED")).isEqualTo(ServiceRecordStatus.ENDED);
    }
}
