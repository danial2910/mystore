package com.store.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class UserTest {

    @Test
    void settersAndGetters_roundTrip() {
        User user = new User();
        user.setId(1);
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setPassword("secret");
        user.setRoles("ROLE_USER");
        user.setFullName("Test User");
        user.setIcNumber("900101-14-1234");
        user.setPhoneNumber("011-12345678");
        user.setAddress("123 Jalan Test, KL");
        user.setProfilePicture("avatar.jpg");

        assertThat(user.getId()).isEqualTo(1);
        assertThat(user.getUsername()).isEqualTo("testuser");
        assertThat(user.getEmail()).isEqualTo("test@example.com");
        assertThat(user.getPassword()).isEqualTo("secret");
        assertThat(user.getRoles()).isEqualTo("ROLE_USER");
        assertThat(user.getFullName()).isEqualTo("Test User");
        assertThat(user.getIcNumber()).isEqualTo("900101-14-1234");
        assertThat(user.getPhoneNumber()).isEqualTo("011-12345678");
        assertThat(user.getAddress()).isEqualTo("123 Jalan Test, KL");
        assertThat(user.getProfilePicture()).isEqualTo("avatar.jpg");
    }

    @Test
    void newUser_fieldsAreNull() {
        User user = new User();
        assertThat(user.getUsername()).isNull();
        assertThat(user.getEmail()).isNull();
        assertThat(user.getFullName()).isNull();
        assertThat(user.getRoles()).isNull();
    }
}
