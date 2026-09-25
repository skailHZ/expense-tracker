package com.example.expense_tracker.dto;

import com.example.expense_tracker.entity.enums.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Username cannot be empty")
        @Size(min = 3, max = 50)
        String username,

        @NotBlank(message = "Password cannot be empty")
        @Size(min = 6, max = 255)
        String password,

        @NotNull(message = "Role is required")
        Role role
) {}