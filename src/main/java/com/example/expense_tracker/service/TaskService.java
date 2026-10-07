package com.example.expense_tracker.service;

import com.example.expense_tracker.dto.TaskCreateDto;
import com.example.expense_tracker.dto.TaskDto;
import com.example.expense_tracker.entity.Project;
import com.example.expense_tracker.entity.Task;
import com.example.expense_tracker.entity.User;
import com.example.expense_tracker.exception.ResourceNotFoundException;
import com.example.expense_tracker.mapper.TaskMapper;
import com.example.expense_tracker.repository.ProjectRepository;
import com.example.expense_tracker.repository.TaskRepository;
import com.example.expense_tracker.repository.UserRepository;
import com.example.expense_tracker.security.ProjectAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final TaskMapper taskMapper;
    private final ProjectAccessService accessService;

    @Transactional(readOnly = true)
    public Page<TaskDto> getTasksByProjectId(Long projectId, Pageable pageable, String username) {
        accessService.checkAccess(projectId, username);
        return taskRepository.findAllByProjectId(projectId, pageable)
                .map(taskMapper::toDto);
    }

    @Transactional
    public TaskDto createTask(Long projectId, TaskCreateDto dto, String username) {
        accessService.checkAccess(projectId, username);

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found: " + projectId));

        User employee = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

        Task task = taskMapper.toEntity(dto);
        task.setProject(project);
        task.setEmployee(employee);

        Task savedTask = taskRepository.save(task);
        return taskMapper.toDto(savedTask);
    }
}