package com.expiryguard.auth.service;

import com.expiryguard.auth.dto.AuthResponse;
import com.expiryguard.auth.dto.LoginRequest;
import com.expiryguard.auth.dto.RegisterRequest;
import com.expiryguard.auth.dto.UserDto;
import com.expiryguard.auth.model.Role;
import com.expiryguard.auth.model.User;
import com.expiryguard.auth.repository.UserRepository;
import com.expiryguard.common.exception.BusinessException;
import com.expiryguard.common.exception.ResourceNotFoundException;
import com.expiryguard.disposal.repository.DisposalRepository;
import com.expiryguard.pos.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final DisposalRepository disposalRepository;
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
                mapToDto(user)
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
                mapToDto(user)
        );
    }

    public UserDto getCurrentUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return mapToDto(user);
    }

    public List<UserDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::mapToDto)
                .toList();
    }

    @Transactional
    public String deleteUser(Long targetUserId, String currentAdminUsername) {
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + targetUserId));

        if (targetUser.getUsername().equalsIgnoreCase(currentAdminUsername)) {
            throw new BusinessException("Action rejected: Administrators cannot delete their own account.");
        }

        boolean hasOrders = orderRepository.existsByCashierId(targetUserId);
        boolean hasDisposals = disposalRepository.existsByApprovedById(targetUserId);

        if (hasOrders || hasDisposals) {
            targetUser.setActive(false);
            userRepository.save(targetUser);
            return "Staff account for '" + targetUser.getFullName() + "' was deactivated to preserve historical transaction audit logs.";
        }

        userRepository.delete(targetUser);
        return "Staff account for '" + targetUser.getFullName() + "' was removed successfully.";
    }

    @Transactional
    public UserDto toggleUserStatus(Long targetUserId, String currentAdminUsername) {
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + targetUserId));

        if (targetUser.getUsername().equalsIgnoreCase(currentAdminUsername)) {
            throw new BusinessException("Action rejected: Administrators cannot deactivate their own account.");
        }

        targetUser.setActive(!targetUser.isActive());
        User saved = userRepository.save(targetUser);
        return mapToDto(saved);
    }

    private UserDto mapToDto(User user) {
        return new UserDto(
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                user.getRole(),
                user.isActive(),
                user.getCreatedAt()
        );
    }
}
