package com.store.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

class UserProfileDTOTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void settersAndGetters_roundTrip() {
        MockMultipartFile file = new MockMultipartFile("profileImageFile", new byte[0]);

        UserProfileDTO dto = new UserProfileDTO();
        dto.setFullName("Ahmad Ali");
        dto.setIcNumber("900101-14-1234");
        dto.setPhoneNumber("011-12345678");
        dto.setAddress("123 Jalan Test, KL");
        dto.setProfileImageFile(file);

        assertThat(dto.getFullName()).isEqualTo("Ahmad Ali");
        assertThat(dto.getIcNumber()).isEqualTo("900101-14-1234");
        assertThat(dto.getPhoneNumber()).isEqualTo("011-12345678");
        assertThat(dto.getAddress()).isEqualTo("123 Jalan Test, KL");
        assertThat(dto.getProfileImageFile()).isEqualTo(file);
    }

    @Test
    void validation_emptyFullName_hasViolation() {
        UserProfileDTO dto = new UserProfileDTO();
        dto.setFullName("");

        Set<ConstraintViolation<UserProfileDTO>> violations = validator.validate(dto);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("fullName"));
    }

    @Test
    void validation_invalidIcNumber_hasViolation() {
        UserProfileDTO dto = new UserProfileDTO();
        dto.setFullName("Ahmad Ali");
        dto.setIcNumber("abc-xyz");

        Set<ConstraintViolation<UserProfileDTO>> violations = validator.validate(dto);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("icNumber"));
    }

    @Test
    void validation_invalidPhoneNumber_hasViolation() {
        UserProfileDTO dto = new UserProfileDTO();
        dto.setFullName("Ahmad Ali");
        dto.setPhoneNumber("abc-defgh");

        Set<ConstraintViolation<UserProfileDTO>> violations = validator.validate(dto);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("phoneNumber"));
    }

    @Test
    void validation_validData_noViolations() {
        UserProfileDTO dto = new UserProfileDTO();
        dto.setFullName("Ahmad Ali");
        dto.setIcNumber("900101-14-1234");
        dto.setPhoneNumber("011-12345678");
        dto.setAddress("123 Jalan Test, KL");

        Set<ConstraintViolation<UserProfileDTO>> violations = validator.validate(dto);
        assertThat(violations).isEmpty();
    }
}
