package com.example.expense_tracker.service;

import com.example.expense_tracker.dto.AuthenticationRequest;
import com.example.expense_tracker.dto.AuthenticationResponse;
import com.example.expense_tracker.dto.RegisterRequest;
import com.example.expense_tracker.entity.User;
import com.example.expense_tracker.entity.enums.Role;
import com.example.expense_tracker.exception.ConflictException;
import com.example.expense_tracker.repository.UserRepository;
import com.example.expense_tracker.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthenticationResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new ConflictException("Username already exists");
        }

        User user = new User();
        user.setUsername(request.username());
        user.setPassword(passwordEncoder.encode(request.password())); // Защита пароля
        user.setRole(Role.ROLE_EMPLOYEE); // Админа нельзя создать через открытый эндпоинт

        userRepository.save(user);

        String jwtToken = jwtService.generateToken(user);
        return new AuthenticationResponse(jwtToken);
    }

    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        // Менеджер Spring Security сам проверит совпадение BCrypt хэшей.
        // Если пароль неверный — выбросит BadCredentialsException
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.username(),
                        request.password()
                )
        );

        User user = userRepository.findByUsername(request.username())
                .orElseThrow();

        String jwtToken = jwtService.generateToken(user);
        return new AuthenticationResponse(jwtToken);
    }
}