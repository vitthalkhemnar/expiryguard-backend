package com.expiryguard.auth.service;

import com.expiryguard.auth.dto.AuthResponse;
import com.expiryguard.auth.dto.LoginRequest;
import com.expiryguard.auth.dto.RegisterRequest;
import com.expiryguard.auth.dto.UserDto;
import com.expiryguard.auth.model.Role;
import com.expiryguard.auth.model.User;
import com.expiryguard.auth.repository.UserRepository;
import com.expiryguard.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );

        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        String token = jwtService.generateToken(user, Map.of(
                "role", user.getRole().name(),
                "fullName", user.getFullName()
        ));

        return new AuthResponse(
                token,
                new UserDto(user.getId(), user.getUsername(), user.getFullName(), user.getRole())
        );
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new BusinessException("Username '" + request.username() + "' is already taken");
        }

        // If it's the first registered user, grant them ROLE_ADMIN automatically
        Role role = request.role() != null ? request.role() : Role.ROLE_CASHIER;
        if (userRepository.count() == 0) {
            role = Role.ROLE_ADMIN;
        }

        User user = User.builder()
                .username(request.username())
                .password(passwordEncoder.encode(request.password()))
                .fullName(request.fullName())
                .role(role)
                .active(true)
                .build();

        user = userRepository.save(user);

        String token = jwtService.generateToken(user, Map.of(
                "role", user.getRole().name(),
                "fullName", user.getFullName()
        ));

        return new AuthResponse(
                token,
                new UserDto(user.getId(), user.getUsername(), user.getFullName(), user.getRole())
        );
    }

    public UserDto getCurrentUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return new UserDto(user.getId(), user.getUsername(), user.getFullName(), user.getRole());
    }

    public java.util.List<UserDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(user -> new UserDto(user.getId(), user.getUsername(), user.getFullName(), user.getRole()))
                .toList();
    }
}
