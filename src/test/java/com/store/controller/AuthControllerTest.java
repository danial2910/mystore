package com.store.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;

import com.store.model.User;
import com.store.model.UserDTO;
import com.store.service.UserRepository;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserDetailsService userDetailsService;

    @InjectMocks
    private AuthController authController;

    private Model model;

    @BeforeEach
    void setUp() {
        model = new ExtendedModelMap();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void signupForm_addsUserDTOToModelAndReturnsSignupView() {
        String view = authController.signupForm(model);

        assertThat(view).isEqualTo("auth/signup");
        assertThat(model.getAttribute("userDTO")).isInstanceOf(UserDTO.class);
    }

    @Test
    void signupSubmit_passwordsDoNotMatch_returnsSignupViewWithError() {
        UserDTO userDTO = new UserDTO();
        userDTO.setUsername("newuser");
        userDTO.setEmail("newuser@example.com");
        userDTO.setPassword("password1");
        userDTO.setConfirmPassword("password2");

        when(userRepository.findByUsername("newuser")).thenReturn(Optional.empty());

        BindingResult result = new BeanPropertyBindingResult(userDTO, "userDTO");

        String view = authController.signupSubmit(userDTO, result, model);

        assertThat(view).isEqualTo("auth/signup");
        assertThat(result.hasFieldErrors("confirmPassword")).isTrue();
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void signupSubmit_usernameAlreadyTaken_returnsSignupViewWithError() {
        UserDTO userDTO = new UserDTO();
        userDTO.setUsername("existinguser");
        userDTO.setEmail("existinguser@example.com");
        userDTO.setPassword("password1");
        userDTO.setConfirmPassword("password1");

        when(userRepository.findByUsername("existinguser")).thenReturn(Optional.of(new User()));

        BindingResult result = new BeanPropertyBindingResult(userDTO, "userDTO");

        String view = authController.signupSubmit(userDTO, result, model);

        assertThat(view).isEqualTo("auth/signup");
        assertThat(result.hasFieldErrors("username")).isTrue();
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void signupSubmit_validData_savesUserLogsInAndRedirectsToAccount() {
        UserDTO userDTO = new UserDTO();
        userDTO.setUsername("newuser");
        userDTO.setEmail("newuser@example.com");
        userDTO.setPassword("password1");
        userDTO.setConfirmPassword("password1");

        when(userRepository.findByUsername("newuser")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password1")).thenReturn("encoded-password");

        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername("newuser")
                .password("encoded-password")
                .authorities("ROLE_USER")
                .build();
        when(userDetailsService.loadUserByUsername("newuser")).thenReturn(userDetails);

        BindingResult result = new BeanPropertyBindingResult(userDTO, "userDTO");

        String view = authController.signupSubmit(userDTO, result, model);

        assertThat(view).isEqualTo("redirect:/account");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getUsername()).isEqualTo("newuser");
        assertThat(savedUser.getEmail()).isEqualTo("newuser@example.com");
        assertThat(savedUser.getPassword()).isEqualTo("encoded-password");
        assertThat(savedUser.getRoles()).isEqualTo("ROLE_USER");

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.isAuthenticated()).isTrue();
        assertThat(auth.getPrincipal()).isEqualTo(userDetails);
    }

    @Test
    void loginPage_returnsLoginView() {
        String view = authController.loginPage();

        assertThat(view).isEqualTo("auth/login");
    }

    @Test
    void accountPage_whenNotAuthenticated_redirectsToLogin() {
        SecurityContextHolder.clearContext();

        String view = authController.accountPage(model);

        assertThat(view).isEqualTo("redirect:/login");
    }

    @Test
    void accountPage_whenAuthenticated_returnsAccountViewWithPrincipal() {
        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername("existinguser")
                .password("encoded-password")
                .authorities("ROLE_USER")
                .build();
        Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        String view = authController.accountPage(model);

        assertThat(view).isEqualTo("account");
        assertThat(model.getAttribute("principal")).isEqualTo(userDetails);
    }

    @Test
    void accountPage_whenAuthPresentButNotAuthenticated_redirectsToLogin() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(false);
        SecurityContextHolder.getContext().setAuthentication(auth);

        String view = authController.accountPage(model);

        assertThat(view).isEqualTo("redirect:/login");
    }
}
