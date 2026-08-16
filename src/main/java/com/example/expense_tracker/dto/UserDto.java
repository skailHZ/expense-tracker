package com.example.expense_tracker.dto;

import com.example.expense_tracker.entity.enums.Role;
import java.time.Instant;

// Обрати внимание: пароля здесь НЕТ!
public record UserDto(
        Long id,
        String username,
        Role role,
        Instant createdAt
) {}