package com.example.expense_tracker.dto;

import jakarta.validation.constraints.NotBlank;

public record AddMemberRequest(
        @NotBlank(message = "Username cannot be empty")
        String username
) {}
