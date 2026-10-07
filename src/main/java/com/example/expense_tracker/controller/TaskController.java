package com.example.expense_tracker.controller;

import com.example.expense_tracker.dto.TaskCreateDto;
import com.example.expense_tracker.dto.TaskDto;
import com.example.expense_tracker.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @GetMapping
    public ResponseEntity<Page<TaskDto>> getTasksByProject(
            @PathVariable Long projectId,
            Pageable pageable,
            Principal principal) {

        Page<TaskDto> tasks = taskService.getTasksByProjectId(projectId, pageable, principal.getName());
        return ResponseEntity.ok(tasks);
    }

    @PostMapping
    public ResponseEntity<TaskDto> createTask(
            @PathVariable Long projectId,
            @Valid @RequestBody TaskCreateDto dto,
            Principal principal
    ) {
        TaskDto createdTask = taskService.createTask(projectId, dto, principal.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(createdTask);
    }
}