package com.example.expense_tracker.dto;

import com.example.expense_tracker.entity.enums.ExpenseStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record ExpenseDto(
        Long id,
        BigDecimal amount,
        String currency,
        String description,
        ExpenseStatus status,
        Long projectId,
        Long employeeId,
        Instant createdAt
) {}
