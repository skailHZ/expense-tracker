package com.example.expense_tracker.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProjectCreateDto(
        @NotBlank(message = "Название проекта не может быть пустым")
        @Size(min = 2, max = 255, message = "Название должно содержать от 2 до 255 символов")
        String name,

        String description
) {}