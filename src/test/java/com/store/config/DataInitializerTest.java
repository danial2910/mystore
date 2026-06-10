package com.store.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.store.model.User;
import com.store.service.UserRepository;

@ExtendWith(MockitoExtension.class)
class DataInitializerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private DataInitializer dataInitializer;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(dataInitializer, "adminUsername", "admin");
        ReflectionTestUtils.setField(dataInitializer, "adminPassword", "admin123");
        ReflectionTestUtils.setField(dataInitializer, "adminEmail", "admin@inventory.local");
    }

    @Test
    void run_adminDoesNotExist_createsAdminUser() {
        when(userRepository.findByUsername("admin")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("admin123")).thenReturn("encoded-password");

        dataInitializer.run();

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getUsername()).isEqualTo("admin");
        assertThat(savedUser.getEmail()).isEqualTo("admin@inventory.local");
        assertThat(savedUser.getPassword()).isEqualTo("encoded-password");
        assertThat(savedUser.getRoles()).isEqualTo("ROLE_USER,ROLE_ADMIN");
    }

    @Test
    void run_adminAlreadyExists_doesNotCreateUser() {
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(new User()));

        dataInitializer.run();

        verify(userRepository, never()).save(any(User.class));
    }
}
