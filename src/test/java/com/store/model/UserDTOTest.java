package com.store.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

class UserDTOTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void settersAndGetters_roundTrip() {
        UserDTO dto = new UserDTO();
        dto.setUsername("alice");
        dto.setEmail("alice@example.com");
        dto.setPassword("password1");
        dto.setConfirmPassword("password1");

        assertThat(dto.getUsername()).isEqualTo("alice");
        assertThat(dto.getEmail()).isEqualTo("alice@example.com");
        assertThat(dto.getPassword()).isEqualTo("password1");
        assertThat(dto.getConfirmPassword()).isEqualTo("password1");
    }

    @Test
    void validation_emptyUsername_hasViolation() {
        UserDTO dto = new UserDTO();
        dto.setUsername("");
        dto.setEmail("alice@example.com");
        dto.setPassword("password1");

        Set<ConstraintViolation<UserDTO>> violations = validator.validate(dto);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("username"));
    }

    @Test
    void validation_invalidEmail_hasViolation() {
        UserDTO dto = new UserDTO();
        dto.setUsername("alice");
        dto.setEmail("not-an-email");
        dto.setPassword("password1");

        Set<ConstraintViolation<UserDTO>> violations = validator.validate(dto);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("email"));
    }

    @Test
    void validation_shortPassword_hasViolation() {
        UserDTO dto = new UserDTO();
        dto.setUsername("alice");
        dto.setEmail("alice@example.com");
        dto.setPassword("abc");

        Set<ConstraintViolation<UserDTO>> violations = validator.validate(dto);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("password"));
    }

    @Test
    void validation_validData_noViolations() {
        UserDTO dto = new UserDTO();
        dto.setUsername("alice");
        dto.setEmail("alice@example.com");
        dto.setPassword("password1");
        dto.setConfirmPassword("password1");

        Set<ConstraintViolation<UserDTO>> violations = validator.validate(dto);
        assertThat(violations).isEmpty();
    }
}
