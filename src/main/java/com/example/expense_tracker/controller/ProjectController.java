package com.example.expense_tracker.controller;

import com.example.expense_tracker.dto.AddMemberRequest;
import com.example.expense_tracker.dto.ProjectCreateDto;
import com.example.expense_tracker.dto.ProjectDto;
import com.example.expense_tracker.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @GetMapping("/{id}")
    public ResponseEntity<ProjectDto> getProjectById(@PathVariable Long id, Principal principal) {
        ProjectDto project = projectService.getProjectById(id, principal.getName());
        return ResponseEntity.ok(project);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')") // Доступ строго для администраторов
    public ResponseEntity<ProjectDto> createProject(
            @Valid @RequestBody ProjectCreateDto dto,
            Principal principal // Spring сам инжектит сюда данные из JWT
    ) {
        // principal.getName() вернет username пользователя, чей токен был прислан
        ProjectDto createdProject = projectService.createProject(dto, principal.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(createdProject);
    }

    // Выдать сотруднику доступ к проекту. Только админ этого проекта (проверяется в сервисе)
    @PostMapping("/{id}/members")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> addMember(
            @PathVariable Long id,
            @Valid @RequestBody AddMemberRequest request,
            Principal principal
    ) {
        projectService.addMember(id, request.username(), principal.getName());
        return ResponseEntity.noContent().build();
    }
}
