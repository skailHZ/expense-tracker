package com.example.expense_tracker.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Роли здесь нет намеренно: самостоятельная регистрация всегда создает ROLE_EMPLOYEE
public record RegisterRequest(
        @NotBlank(message = "Username cannot be empty")
        @Size(min = 3, max = 50)
        String username,

        @NotBlank(message = "Password cannot be empty")
        @Size(min = 6, max = 255)
        String password
) {}
