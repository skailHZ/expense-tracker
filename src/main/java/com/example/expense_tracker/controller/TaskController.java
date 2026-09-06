package com.example.expense_tracker.controller;

import com.example.expense_tracker.dto.TaskDto;
import com.example.expense_tracker.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    // В Spring Web аргумент Pageable автоматически собирается из query-параметров URL
    // (например: ?page=0&size=20&sort=createdAt,desc)
    @GetMapping
    public ResponseEntity<Page<TaskDto>> getTasksByProject(
            @PathVariable Long projectId,
            Pageable pageable) {

        Page<TaskDto> tasks = taskService.getTasksByProjectId(projectId, pageable);
        return ResponseEntity.ok(tasks);
    }
}