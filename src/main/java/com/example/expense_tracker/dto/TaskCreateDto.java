package com.example.expense_tracker.dto;

import com.example.expense_tracker.entity.enums.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TaskCreateDto(
        @NotBlank(message = "Название задачи не может быть пустым")
        @Size(min = 2, max = 255)
        String title,

        @NotNull(message = "Статус обязателен")
        TaskStatus status
) {}