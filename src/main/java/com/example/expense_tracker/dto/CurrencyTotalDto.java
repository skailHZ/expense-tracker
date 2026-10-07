package com.example.expense_tracker.dto;

import java.math.BigDecimal;

public record CurrencyTotalDto(String currency, BigDecimal amount) {}
