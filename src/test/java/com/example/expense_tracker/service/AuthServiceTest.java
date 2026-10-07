package com.example.expense_tracker.service;

import com.example.expense_tracker.dto.RegisterRequest;
import com.example.expense_tracker.entity.User;
import com.example.expense_tracker.entity.enums.Role;
import com.example.expense_tracker.exception.ConflictException;
import com.example.expense_tracker.repository.UserRepository;
import com.example.expense_tracker.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    @Test
    void register_AlwaysCreatesEmployee_AndHashesPassword() {
        RegisterRequest request = new RegisterRequest("newbie", "secret123");
        when(userRepository.existsByUsername("newbie")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("hashed");
        when(jwtService.generateToken(any(User.class))).thenReturn("jwt");

        authService.register(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals(Role.ROLE_EMPLOYEE, captor.getValue().getRole());
        assertEquals("hashed", captor.getValue().getPassword());
    }

    @Test
    void register_ThrowsConflict_WhenUsernameTaken() {
        when(userRepository.existsByUsername("taken")).thenReturn(true);

        assertThrows(ConflictException.class,
                () -> authService.register(new RegisterRequest("taken", "secret123")));

        verify(userRepository, never()).save(any());
    }
}
