package com.example.expense_tracker.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record ExpenseDto(
        Long id,
        BigDecimal amount,
        String description,
        Long projectId,
        Long employeeId,
        Instant createdAt
) {}