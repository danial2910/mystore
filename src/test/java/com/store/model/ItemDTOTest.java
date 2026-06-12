package com.store.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

class ItemDTOTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void settersAndGetters_roundTrip() {
        LocalDateTime start = LocalDateTime.of(2026, 1, 1, 10, 0);
        LocalDateTime end = LocalDateTime.of(2026, 1, 1, 14, 0);
        MockMultipartFile file = new MockMultipartFile("imageFile", new byte[0]);

        ItemDTO dto = new ItemDTO();
        dto.setName("Widget");
        dto.setBrand("Acme");
        dto.setCategory("electronics");
        dto.setQuantity(5);
        dto.setDescription("A description.");
        dto.setImageFile(file);
        dto.setStorageStartTime(start);
        dto.setStorageEndTime(end);

        assertThat(dto.getName()).isEqualTo("Widget");
        assertThat(dto.getBrand()).isEqualTo("Acme");
        assertThat(dto.getCategory()).isEqualTo("electronics");
        assertThat(dto.getQuantity()).isEqualTo(5);
        assertThat(dto.getDescription()).isEqualTo("A description.");
        assertThat(dto.getImageFile()).isEqualTo(file);
        assertThat(dto.getStorageStartTime()).isEqualTo(start);
        assertThat(dto.getStorageEndTime()).isEqualTo(end);
    }

    @Test
    void validation_emptyCategory_hasViolation() {
        ItemDTO dto = new ItemDTO();
        dto.setCategory("");
        dto.setDescription("Long enough description here.");
        dto.setStorageStartTime(LocalDateTime.now().plusHours(1));
        dto.setStorageEndTime(LocalDateTime.now().plusHours(5));

        Set<ConstraintViolation<ItemDTO>> violations = validator.validate(dto);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("category"));
    }

    @Test
    void validation_shortDescription_hasViolation() {
        ItemDTO dto = new ItemDTO();
        dto.setCategory("electronics");
        dto.setDescription("Too short");
        dto.setStorageStartTime(LocalDateTime.now().plusHours(1));
        dto.setStorageEndTime(LocalDateTime.now().plusHours(5));

        Set<ConstraintViolation<ItemDTO>> violations = validator.validate(dto);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("description"));
    }

    @Test
    void validation_nullStorageTimes_hasViolations() {
        ItemDTO dto = new ItemDTO();
        dto.setCategory("electronics");
        dto.setDescription("A description that is long enough for validation.");

        Set<ConstraintViolation<ItemDTO>> violations = validator.validate(dto);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("storageStartTime"));
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("storageEndTime"));
    }

    @Test
    void validation_negativeQuantity_hasViolation() {
        ItemDTO dto = new ItemDTO();
        dto.setCategory("electronics");
        dto.setDescription("A description that is long enough for validation.");
        dto.setQuantity(-1);
        dto.setStorageStartTime(LocalDateTime.now().plusHours(1));
        dto.setStorageEndTime(LocalDateTime.now().plusHours(5));

        Set<ConstraintViolation<ItemDTO>> violations = validator.validate(dto);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("quantity"));
    }
}
