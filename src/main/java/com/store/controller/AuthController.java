package com.store.controller;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.multipart.MultipartFile;

import com.store.model.User;
import com.store.model.UserDTO;
import com.store.model.UserProfileDTO;
import com.store.service.UserRepository;

import jakarta.validation.Valid;

@Controller
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Value("${app.upload.dir}")
    private String uploadDir;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserDetailsService userDetailsService;

    @GetMapping("/signup")
    public String signupForm(Model model) {
        model.addAttribute("userDTO", new UserDTO());
        return "auth/signup";
    }

    @PostMapping("/signup")
    public String signupSubmit(@Valid @ModelAttribute UserDTO userDTO, BindingResult result, Model model) {
        if (!userDTO.getPassword().equals(userDTO.getConfirmPassword())) {
            result.addError(new FieldError("userDTO", "confirmPassword", "Passwords do not match"));
        }

        if (userRepository.findByUsername(userDTO.getUsername()).isPresent()) {
            result.addError(new FieldError("userDTO", "username", "Username already taken"));
        }

        if (result.hasErrors()) {
            return "auth/signup";
        }

        User user = new User();
        user.setUsername(userDTO.getUsername());
        user.setEmail(userDTO.getEmail());
        user.setPassword(passwordEncoder.encode(userDTO.getPassword()));
        user.setRoles("ROLE_USER");

        userRepository.save(user);

        UserDetails ud = userDetailsService.loadUserByUsername(user.getUsername());
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(ud, null, ud.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        return "redirect:/account";
    }

    @GetMapping("/login")
    public String loginPage() {
        return "auth/login";
    }

    @GetMapping("/account")
    public String accountPage(Model model, Authentication authentication) {
        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("User not found"));
        model.addAttribute("user", user);
        model.addAttribute("profileComplete", isProfileComplete(user));
        return "account";
    }

    @GetMapping("/account/edit")
    public String editProfileForm(Model model, Authentication authentication) {
        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("User not found"));

        UserProfileDTO dto = new UserProfileDTO();
        dto.setFullName(user.getFullName());
        dto.setIcNumber(user.getIcNumber());
        dto.setPhoneNumber(user.getPhoneNumber());
        dto.setAddress(user.getAddress());

        model.addAttribute("profileDTO", dto);
        model.addAttribute("user", user);
        return "account-edit";
    }

    @PostMapping("/account/edit")
    public String updateProfile(@Valid @ModelAttribute("profileDTO") UserProfileDTO dto, BindingResult result,
                                 Authentication authentication, Model model) {
        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("User not found"));

        if (result.hasErrors()) {
            model.addAttribute("user", user);
            return "account-edit";
        }

        if (dto.getProfileImageFile() != null && !dto.getProfileImageFile().isEmpty()) {
            deleteProfileImage(user.getProfilePicture());
            user.setProfilePicture(storeProfileImage(dto.getProfileImageFile()));
        }

        user.setFullName(dto.getFullName());
        user.setIcNumber(dto.getIcNumber());
        user.setPhoneNumber(dto.getPhoneNumber());
        user.setAddress(dto.getAddress());

        userRepository.save(user);

        return "redirect:/account";
    }

    private boolean isProfileComplete(User user) {
        return user.getFullName() != null && !user.getFullName().isBlank()
                && user.getIcNumber() != null && !user.getIcNumber().isBlank()
                && user.getPhoneNumber() != null && !user.getPhoneNumber().isBlank()
                && user.getAddress() != null && !user.getAddress().isBlank();
    }

    private String storeProfileImage(MultipartFile imageFile) {
        if (imageFile == null || imageFile.isEmpty()) {
            return null;
        }

        String originalFilename = imageFile.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf('.'));
        }
        String storageFileName = UUID.randomUUID() + extension;

        try {
            Path uploadPath = Paths.get(uploadDir).resolve("profiles");
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            try (InputStream inputStream = imageFile.getInputStream()) {
                Files.copy(inputStream, uploadPath.resolve(storageFileName), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ex) {
            logger.error("Failed to store profile image {}", storageFileName, ex);
        }

        return storageFileName;
    }

    private void deleteProfileImage(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return;
        }
        try {
            Files.deleteIfExists(Paths.get(uploadDir).resolve("profiles").resolve(fileName));
        } catch (IOException ex) {
            logger.error("Failed to delete profile image {}", fileName, ex);
        }
    }
}
