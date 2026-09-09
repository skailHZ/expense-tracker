package com.example.expense_tracker.service;

import com.example.expense_tracker.dto.ProjectCreateDto;
import com.example.expense_tracker.dto.ProjectDto;
import com.example.expense_tracker.entity.Project;
import com.example.expense_tracker.entity.User;
import com.example.expense_tracker.exception.ResourceNotFoundException;
import com.example.expense_tracker.mapper.ProjectMapper;
import com.example.expense_tracker.repository.ProjectRepository;
import com.example.expense_tracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ProjectMapper projectMapper;

    @Transactional(readOnly = true)
    public ProjectDto getProjectById(Long id) {
        return projectRepository.findById(id)
                .map(projectMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Project with id " + id + " not found"));
    }

    @Transactional // Здесь readOnly = false, так как мы меняем состояние БД
    public ProjectDto createProject(ProjectCreateDto dto) {
        User admin = userRepository.findById(dto.adminId())
                .orElseThrow(() -> new ResourceNotFoundException("User with id " + dto.adminId() + " not found"));

        Project project = projectMapper.toEntity(dto);
        project.setAdmin(admin); // Устанавливаем связь

        Project savedProject = projectRepository.save(project);
        return projectMapper.toDto(savedProject);
    }
}