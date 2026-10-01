package com.example.expense_tracker.controller;

import com.example.expense_tracker.dto.ProjectCreateDto;
import com.example.expense_tracker.dto.ProjectDto;
import com.example.expense_tracker.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @GetMapping("/{id}")
    public ResponseEntity<ProjectDto> getProjectById(@PathVariable Long id) {
        ProjectDto project = projectService.getProjectById(id);
        return ResponseEntity.ok(project);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')") // Доступ строго для администраторов
    public ResponseEntity<ProjectDto> createProject(
            @Valid @RequestBody ProjectCreateDto dto,
            java.security.Principal principal // Spring сам инжектит сюда данные из JWT
    ) {
        // principal.getName() вернет username пользователя, чей токен был прислан
        ProjectDto createdProject = projectService.createProject(dto, principal.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(createdProject);
    }
}