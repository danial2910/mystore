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
import org.springframework.mock.web.MockMultipartFile;
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
import com.store.model.UserProfileDTO;
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

    // ── Signup ────────────────────────────────────────────────────────────────

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

    // ── Login ─────────────────────────────────────────────────────────────────

    @Test
    void loginPage_returnsLoginView() {
        String view = authController.loginPage();

        assertThat(view).isEqualTo("auth/login");
    }

    // ── Account page ──────────────────────────────────────────────────────────

    @Test
    void accountPage_incompleteProfile_addsUserAndProfileCompleteFalse() {
        User user = new User();
        user.setUsername("existinguser");
        user.setEmail("existinguser@example.com");

        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("existinguser");
        when(userRepository.findByUsername("existinguser")).thenReturn(Optional.of(user));

        String view = authController.accountPage(model, auth);

        assertThat(view).isEqualTo("account");
        assertThat(model.getAttribute("user")).isEqualTo(user);
        assertThat(model.getAttribute("profileComplete")).isEqualTo(false);
    }

    @Test
    void accountPage_completeProfile_addsProfileCompleteTrue() {
        User user = new User();
        user.setUsername("fulluser");
        user.setFullName("Ahmad Ali");
        user.setIcNumber("900101-14-1234");
        user.setPhoneNumber("011-12345678");
        user.setAddress("123 Jalan Test, KL");

        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("fulluser");
        when(userRepository.findByUsername("fulluser")).thenReturn(Optional.of(user));

        String view = authController.accountPage(model, auth);

        assertThat(view).isEqualTo("account");
        assertThat(model.getAttribute("profileComplete")).isEqualTo(true);
    }

    // ── Edit profile ──────────────────────────────────────────────────────────

    @Test
    void editProfileForm_returnsEditViewWithPrefilledDTO() {
        User user = new User();
        user.setUsername("testuser");
        user.setFullName("Ahmad Ali");
        user.setIcNumber("900101-14-1234");
        user.setPhoneNumber("011-12345678");
        user.setAddress("123 Jalan Test, KL");

        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("testuser");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        String view = authController.editProfileForm(model, auth);

        assertThat(view).isEqualTo("account-edit");
        UserProfileDTO dto = (UserProfileDTO) model.getAttribute("profileDTO");
        assertThat(dto).isNotNull();
        assertThat(dto.getFullName()).isEqualTo("Ahmad Ali");
        assertThat(dto.getIcNumber()).isEqualTo("900101-14-1234");
        assertThat(dto.getPhoneNumber()).isEqualTo("011-12345678");
        assertThat(dto.getAddress()).isEqualTo("123 Jalan Test, KL");
    }

    @Test
    void updateProfile_validationErrors_returnsEditView() {
        User user = new User();
        user.setUsername("testuser");

        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("testuser");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        UserProfileDTO dto = new UserProfileDTO();
        dto.setFullName("");
        BindingResult result = new BeanPropertyBindingResult(dto, "profileDTO");
        result.rejectValue("fullName", "NotEmpty", "Full name is required");

        String view = authController.updateProfile(dto, result, auth, model);

        assertThat(view).isEqualTo("account-edit");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateProfile_validData_updatesFieldsAndRedirects() {
        User user = new User();
        user.setUsername("testuser");

        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("testuser");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        UserProfileDTO dto = new UserProfileDTO();
        dto.setFullName("Ahmad Ali");
        dto.setIcNumber("900101-14-1234");
        dto.setPhoneNumber("011-12345678");
        dto.setAddress("123 Jalan Test, KL");
        dto.setProfileImageFile(new MockMultipartFile("profileImageFile", "", "image/png", new byte[0]));

        BindingResult result = new BeanPropertyBindingResult(dto, "profileDTO");

        String view = authController.updateProfile(dto, result, auth, model);

        assertThat(view).isEqualTo("redirect:/account");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getFullName()).isEqualTo("Ahmad Ali");
        assertThat(saved.getIcNumber()).isEqualTo("900101-14-1234");
        assertThat(saved.getPhoneNumber()).isEqualTo("011-12345678");
        assertThat(saved.getAddress()).isEqualTo("123 Jalan Test, KL");
    }
}
