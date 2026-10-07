package com.example.expense_tracker.repository;

import com.example.expense_tracker.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    // SELECT EXISTS по проекту и его админу - без загрузки сущностей
    boolean existsByIdAndAdminId(Long id, Long adminId);

    // Проверка членства через таблицу project_members (поле members.id)
    boolean existsByIdAndMembersId(Long id, Long userId);
}
