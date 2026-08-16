package com.example.expense_tracker.dto;

import java.time.Instant;

public record ProjectDto(
        Long id,
        String name,
        String description,
        Long adminId, // Мы отдаем только ID админа, а не всю сущность User
        Instant createdAt
) {}