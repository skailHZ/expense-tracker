package com.example.expense_tracker.dto;

import com.example.expense_tracker.entity.enums.ExpenseStatus;

import java.math.BigDecimal;

public record StatusSummaryDto(ExpenseStatus status, String currency, Long count, BigDecimal total) {}
