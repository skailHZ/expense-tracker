package com.example.expense_tracker.service;

import com.example.expense_tracker.dto.ProjectCreateDto;
import com.example.expense_tracker.dto.ProjectDto;
import com.example.expense_tracker.entity.Project;
import com.example.expense_tracker.entity.User;
import com.example.expense_tracker.exception.ResourceNotFoundException;
import com.example.expense_tracker.mapper.ProjectMapper;
import com.example.expense_tracker.repository.ProjectRepository;
import com.example.expense_tracker.repository.UserRepository;
import com.example.expense_tracker.security.ProjectAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ProjectMapper projectMapper;
    private final ProjectAccessService accessService;

    @Transactional(readOnly = true)
    public ProjectDto getProjectById(Long id, String username) {
        accessService.checkAccess(id, username);
        return projectRepository.findById(id)
                .map(projectMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Project with id " + id + " not found"));
    }

    @Transactional
    public ProjectDto createProject(ProjectCreateDto dto, String username) {
        // Ищем пользователя по логину, который извлекли из токена
        User admin = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

        Project project = projectMapper.toEntity(dto);
        project.setAdmin(admin); // Безопасная привязка

        Project savedProject = projectRepository.save(project);
        return projectMapper.toDto(savedProject);
    }

    @Transactional
    public void addMember(Long projectId, String memberUsername, String callerUsername) {
        accessService.checkProjectAdmin(projectId, callerUsername);

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found: " + projectId));
        User member = userRepository.findByUsername(memberUsername)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + memberUsername));

        // members - это Set, поэтому повторное добавление безопасно (идемпотентно)
        project.getMembers().add(member);
    }
}
