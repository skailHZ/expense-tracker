package com.example.expense_tracker.security;

import com.example.expense_tracker.entity.User;
import com.example.expense_tracker.exception.ResourceNotFoundException;
import com.example.expense_tracker.repository.ProjectRepository;
import com.example.expense_tracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Защита от BOLA (Broken Object Level Authorization): роль говорит, ЧТО пользователь умеет делать,
 * а эта проверка отвечает на вопрос, с КАКИМ конкретным проектом ему можно работать.
 * Доступ есть у админа проекта и у сотрудников, которых он добавил в участники.
 */
@Service
@RequiredArgsConstructor
public class ProjectAccessService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public void checkAccess(Long projectId, String username) {
        User user = findUser(username);
        requireProjectExists(projectId);

        boolean allowed = projectRepository.existsByIdAndAdminId(projectId, user.getId())
                || projectRepository.existsByIdAndMembersId(projectId, user.getId());
        if (!allowed) {
            throw new AccessDeniedException("No access to project " + projectId);
        }
    }

    // Управлять участниками может только админ самого проекта, а не любой ROLE_ADMIN
    @Transactional(readOnly = true)
    public void checkProjectAdmin(Long projectId, String username) {
        User user = findUser(username);
        requireProjectExists(projectId);

        if (!projectRepository.existsByIdAndAdminId(projectId, user.getId())) {
            throw new AccessDeniedException("Only the project admin can do this");
        }
    }

    private User findUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
    }

    private void requireProjectExists(Long projectId) {
        if (!projectRepository.existsById(projectId)) {
            throw new ResourceNotFoundException("Project not found: " + projectId);
        }
    }
}
