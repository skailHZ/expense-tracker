package com.example.expense_tracker.dto;

import com.example.expense_tracker.entity.enums.TaskStatus;
import java.time.Instant;

public record TaskDto(
        Long id,
        String title,
        TaskStatus status,
        Long projectId,
        Long employeeId,
        Instant createdAt
) {}