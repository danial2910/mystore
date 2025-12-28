package com.store.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import com.store.model.User;
import com.store.model.UserDTO;
import com.store.service.UserRepository;

import jakarta.validation.Valid;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

@Controller
public class AuthController {

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

        // auto-login
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
    public String accountPage(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return "redirect:/login";
        }
        Object principal = auth.getPrincipal();
        model.addAttribute("principal", principal);
        return "account";
    }
}
