package com.example.expense_tracker.dto;

import com.example.expense_tracker.entity.enums.ExpenseStatus;

import java.time.Instant;

public record ExpenseStatusChangeDto(
        Long id,
        ExpenseStatus fromStatus,
        ExpenseStatus toStatus,
        String changedBy,
        String comment,
        Instant changedAt
) {}
