package com.store.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.store.model.User;
import com.store.service.UserRepository;

@ExtendWith(MockitoExtension.class)
class SecurityConfigTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private SecurityConfig securityConfig;

    @Test
    void passwordEncoder_returnsBCryptEncoder() {
        PasswordEncoder encoder = securityConfig.passwordEncoder();
        String encoded = encoder.encode("password");
        assertThat(encoder.matches("password", encoded)).isTrue();
    }

    @Test
    void userDetailsService_existingUser_returnsUserDetails() {
        User user = new User();
        user.setUsername("alice");
        user.setPassword("hashed");
        user.setRoles("ROLE_USER");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

        UserDetailsService uds = securityConfig.userDetailsService();
        UserDetails details = uds.loadUserByUsername("alice");

        assertThat(details.getUsername()).isEqualTo("alice");
        assertThat(details.getPassword()).isEqualTo("hashed");
        assertThat(details.getAuthorities()).anyMatch(a -> a.getAuthority().equals("ROLE_USER"));
    }

    @Test
    void userDetailsService_nullRoles_defaultsToRoleUser() {
        User user = new User();
        user.setUsername("bob");
        user.setPassword("hashed");
        user.setRoles(null);
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(user));

        UserDetailsService uds = securityConfig.userDetailsService();
        UserDetails details = uds.loadUserByUsername("bob");

        assertThat(details.getAuthorities()).anyMatch(a -> a.getAuthority().equals("ROLE_USER"));
    }

    @Test
    void userDetailsService_unknownUser_throwsUsernameNotFoundException() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        UserDetailsService uds = securityConfig.userDetailsService();

        assertThatThrownBy(() -> uds.loadUserByUsername("ghost"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("ghost");
    }

    @Test
    void userDetailsService_adminUser_hasAdminAuthority() {
        User user = new User();
        user.setUsername("admin");
        user.setPassword("hashed");
        user.setRoles("ROLE_USER,ROLE_ADMIN");
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));

        UserDetailsService uds = securityConfig.userDetailsService();
        UserDetails details = uds.loadUserByUsername("admin");

        assertThat(details.getAuthorities()).anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        assertThat(details.getAuthorities()).anyMatch(a -> a.getAuthority().equals("ROLE_USER"));
    }
}
