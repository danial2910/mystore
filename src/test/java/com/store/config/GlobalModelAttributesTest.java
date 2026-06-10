package com.store.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

class GlobalModelAttributesTest {

    private final GlobalModelAttributes globalModelAttributes = new GlobalModelAttributes();

    @Test
    void isAuthenticated_nullAuthentication_returnsFalse() {
        assertThat(globalModelAttributes.isAuthenticated(null)).isFalse();
    }

    @Test
    void isAuthenticated_notAuthenticated_returnsFalse() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.isAuthenticated()).thenReturn(false);

        assertThat(globalModelAttributes.isAuthenticated(authentication)).isFalse();
    }

    @Test
    void isAuthenticated_anonymousAuthentication_returnsFalse() {
        Authentication authentication = new AnonymousAuthenticationToken(
                "key", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));

        assertThat(globalModelAttributes.isAuthenticated(authentication)).isFalse();
    }

    @Test
    void isAuthenticated_authenticatedNonAnonymousUser_returnsTrue() {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                "user", "password", List.of(new SimpleGrantedAuthority("ROLE_USER")));

        assertThat(globalModelAttributes.isAuthenticated(authentication)).isTrue();
    }

    @Test
    void isAdmin_nullAuthentication_returnsFalse() {
        assertThat(globalModelAttributes.isAdmin(null)).isFalse();
    }

    @Test
    void isAdmin_anonymousAuthentication_returnsFalse() {
        Authentication authentication = new AnonymousAuthenticationToken(
                "key", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));

        assertThat(globalModelAttributes.isAdmin(authentication)).isFalse();
    }

    @Test
    void isAdmin_authenticatedUserWithoutAdminRole_returnsFalse() {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                "user", "password", List.of(new SimpleGrantedAuthority("ROLE_USER")));

        assertThat(globalModelAttributes.isAdmin(authentication)).isFalse();
    }

    @Test
    void isAdmin_authenticatedUserWithAdminRole_returnsTrue() {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                "admin", "password",
                List.of(new SimpleGrantedAuthority("ROLE_USER"), new SimpleGrantedAuthority("ROLE_ADMIN")));

        assertThat(globalModelAttributes.isAdmin(authentication)).isTrue();
    }
}
