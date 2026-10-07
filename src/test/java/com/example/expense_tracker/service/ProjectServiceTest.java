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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// @ExtendWith позволяет использовать аннотации Mockito без поднятия тяжелого Spring Context
@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    // @Mock создает объекты-пустышки (заглушки)
    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ProjectMapper projectMapper;
    @Mock
    private ProjectAccessService accessService;

    // @InjectMocks создает реальный объект сервиса и внедряет в него наши моки
    @InjectMocks
    private ProjectService projectService;

    @Test
    void createProject_Success() {
        // --- GIVEN (Подготовка данных) ---
        String username = "tech_lead";
        ProjectCreateDto createDto = new ProjectCreateDto("Test Project", "Description");

        User mockUser = new User();
        mockUser.setId(1L);
        mockUser.setUsername(username);

        Project mockProject = new Project();
        mockProject.setName("Test Project");

        Project savedProject = new Project();
        savedProject.setId(100L);
        savedProject.setAdmin(mockUser);

        ProjectDto expectedDto = new ProjectDto(100L, "Test Project", "Description", 1L, Instant.now());

        // Настраиваем поведение моков (Stubs)
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(mockUser));
        when(projectMapper.toEntity(createDto)).thenReturn(mockProject);
        when(projectRepository.save(any(Project.class))).thenReturn(savedProject);
        when(projectMapper.toDto(savedProject)).thenReturn(expectedDto);

        // --- WHEN (Вызов тестируемого метода) ---
        ProjectDto actualDto = projectService.createProject(createDto, username);

        // --- THEN (Проверка результатов) ---
        assertNotNull(actualDto);
        assertEquals(100L, actualDto.id());

        // Проверяем, что методы репозитория были реально вызваны ровно 1 раз
        verify(userRepository, times(1)).findByUsername(username);
        verify(projectRepository, times(1)).save(mockProject);
    }

    @Test
    void createProject_ThrowsException_WhenUserNotFound() {
        // --- GIVEN ---
        String username = "ghost_user";
        ProjectCreateDto createDto = new ProjectCreateDto("Test", "Desc");

        // Имитируем отсутствие пользователя в БД
        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

        // --- WHEN & THEN ---
        // Проверяем, что метод выбрасывает нужное исключение
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> projectService.createProject(createDto, username)
        );

        assertEquals("User not found: " + username, exception.getMessage());

        // Гарантируем, что если юзер не найден, сохранение в БД даже не попытается выполниться
        verify(projectRepository, never()).save(any());
    }

    @Test
    void getProjectById_ChecksAccessBeforeLoading() {
        doThrow(new AccessDeniedException("No access")).when(accessService).checkAccess(5L, "stranger");

        assertThrows(AccessDeniedException.class, () -> projectService.getProjectById(5L, "stranger"));

        // Чужой проект не должен даже читаться из БД
        verify(projectRepository, never()).findById(any());
    }

    @Test
    void addMember_AddsUserToProjectMembers() {
        Project project = new Project();
        User employee = new User();
        employee.setUsername("bob");

        when(projectRepository.findById(5L)).thenReturn(Optional.of(project));
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(employee));

        projectService.addMember(5L, "bob", "boss");

        verify(accessService).checkProjectAdmin(5L, "boss");
        assertTrue(project.getMembers().contains(employee));
    }

    @Test
    void addMember_DeniedForNonProjectAdmin() {
        doThrow(new AccessDeniedException("Only the project admin can do this"))
                .when(accessService).checkProjectAdmin(5L, "mallory");

        assertThrows(AccessDeniedException.class, () -> projectService.addMember(5L, "bob", "mallory"));

        verify(projectRepository, never()).findById(any());
    }
}