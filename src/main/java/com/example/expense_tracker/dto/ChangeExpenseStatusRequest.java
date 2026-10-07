package com.example.expense_tracker.dto;

import com.example.expense_tracker.entity.enums.ExpenseStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ChangeExpenseStatusRequest(
        @NotNull(message = "Статус обязателен")
        ExpenseStatus status,

        @Size(max = 500, message = "Комментарий: не более 500 символов")
        String comment
) {}
