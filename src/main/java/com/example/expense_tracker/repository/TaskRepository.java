package com.example.expense_tracker.repository;

import com.example.expense_tracker.entity.Task;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    // @EntityGraph решает проблему N+1. При вызове этого метода Hibernate
    // сделает один JOIN запрос, подтянув сразу и Проект, и Сотрудника.
    @EntityGraph(attributePaths = {"project", "employee"})
    Page<Task> findAllByProjectId(Long projectId, Pageable pageable);
}