package com.example.expense_tracker.dto;

import java.math.BigDecimal;

public record EmployeeSummaryDto(Long employeeId, String username, String currency, Long count, BigDecimal total) {}
