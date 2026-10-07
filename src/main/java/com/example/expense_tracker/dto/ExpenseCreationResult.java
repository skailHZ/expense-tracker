package com.example.expense_tracker.dto;

// replayed = true, если вернули ранее созданный расход по повторному Idempotency-Key
public record ExpenseCreationResult(ExpenseDto expense, boolean replayed) {}
