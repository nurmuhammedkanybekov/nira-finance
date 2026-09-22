package com.nira.finance.service;

import com.nira.finance.dto.AuthResponse;
import com.nira.finance.dto.LoginRequest;
import com.nira.finance.dto.RegisterRequest;
import com.nira.finance.model.User;
import com.nira.finance.repository.UserRepository;
import com.nira.finance.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AuthResponse register(RegisterRequest request) {
        userRepository.findByEmail(request.getEmail()).ifPresent(u -> {
            throw new IllegalArgumentException("An account with this email already exists");
        });

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setDisplayName(request.getDisplayName());
        userRepository.save(user);
        log.info("Registered new user {} ({})", user.getId(), user.getEmail());

        String token = jwtService.generateToken(user.getId(), user.getEmail());
        return new AuthResponse(token, user.getDisplayName());
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            log.warn("Failed login attempt for {}", request.getEmail());
            throw new IllegalArgumentException("Invalid email or password");
        }

        log.info("User {} logged in", user.getId());
        String token = jwtService.generateToken(user.getId(), user.getEmail());
        return new AuthResponse(token, user.getDisplayName());
    }
}
