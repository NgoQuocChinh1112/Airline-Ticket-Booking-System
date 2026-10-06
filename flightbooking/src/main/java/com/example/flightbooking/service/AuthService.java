package com.example.flightbooking.service;

import com.example.flightbooking.dto.AuthResponse;
import com.example.flightbooking.dto.LoginRequest;
import com.example.flightbooking.dto.RegisterRequest;
import com.example.flightbooking.entity.User;
import com.example.flightbooking.exception.ApiException;
import com.example.flightbooking.repository.UserRepository;
import com.example.flightbooking.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.getEmail())) {
            throw ApiException.conflict("Email đã được sử dụng");
        }

        User user = new User();
        user.setEmail(req.getEmail());
        user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        user.setFullName(req.getFullName());
        user = userRepository.save(user);

        String token = jwtService.generateToken(user.getId().toString(), user.getEmail());
        return new AuthResponse(token, user.getId().toString(), user.getEmail());
    }

    public AuthResponse login(LoginRequest req) {
        User user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> ApiException.unauthorized("Email hoặc mật khẩu không đúng"));

        if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            throw ApiException.unauthorized("Email hoặc mật khẩu không đúng");
        }

        String token = jwtService.generateToken(user.getId().toString(), user.getEmail());
        return new AuthResponse(token, user.getId().toString(), user.getEmail());
    }
}
