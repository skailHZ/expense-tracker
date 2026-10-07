package com.example.expense_tracker.security;

import com.example.expense_tracker.entity.User;
import com.example.expense_tracker.exception.ResourceNotFoundException;
import com.example.expense_tracker.repository.ProjectRepository;
import com.example.expense_tracker.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class ProjectAccessServiceTest {

    private static final Long PROJECT_ID = 10L;
    private static final Long USER_ID = 1L;
    private static final String USERNAME = "alice";

    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ProjectAccessService accessService;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setId(USER_ID);
        user.setUsername(USERNAME);
        lenient().when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user));
        lenient().when(projectRepository.existsById(PROJECT_ID)).thenReturn(true);
    }

    @Test
    void checkAccess_AllowsProjectAdmin() {
        lenient().when(projectRepository.existsByIdAndAdminId(PROJECT_ID, USER_ID)).thenReturn(true);

        assertDoesNotThrow(() -> accessService.checkAccess(PROJECT_ID, USERNAME));
    }

    @Test
    void checkAccess_AllowsProjectMember() {
        lenient().when(projectRepository.existsByIdAndAdminId(PROJECT_ID, USER_ID)).thenReturn(false);
        lenient().when(projectRepository.existsByIdAndMembersId(PROJECT_ID, USER_ID)).thenReturn(true);

        assertDoesNotThrow(() -> accessService.checkAccess(PROJECT_ID, USERNAME));
    }

    @Test
    void checkAccess_DeniesStranger() {
        lenient().when(projectRepository.existsByIdAndAdminId(PROJECT_ID, USER_ID)).thenReturn(false);
        lenient().when(projectRepository.existsByIdAndMembersId(PROJECT_ID, USER_ID)).thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> accessService.checkAccess(PROJECT_ID, USERNAME));
    }

    @Test
    void checkAccess_ThrowsNotFound_WhenProjectMissing() {
        lenient().when(projectRepository.existsById(PROJECT_ID)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> accessService.checkAccess(PROJECT_ID, USERNAME));
    }

    @Test
    void checkProjectAdmin_DeniesMemberWhoIsNotAdmin() {
        lenient().when(projectRepository.existsByIdAndAdminId(PROJECT_ID, USER_ID)).thenReturn(false);
        lenient().when(projectRepository.existsByIdAndMembersId(PROJECT_ID, USER_ID)).thenReturn(true);

        assertThrows(AccessDeniedException.class, () -> accessService.checkProjectAdmin(PROJECT_ID, USERNAME));
    }

    @Test
    void checkProjectAdmin_AllowsProjectAdmin() {
        lenient().when(projectRepository.existsByIdAndAdminId(PROJECT_ID, USER_ID)).thenReturn(true);

        assertDoesNotThrow(() -> accessService.checkProjectAdmin(PROJECT_ID, USERNAME));
    }
}
