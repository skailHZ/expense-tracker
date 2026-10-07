package com.example.expense_tracker.repository;

import com.example.expense_tracker.entity.Task;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    // @EntityGraph здесь не нужен: TaskDto содержит только id проекта и сотрудника, а id лениво загруженной
    // связи Hibernate отдает из прокси без запроса в БД. Граф лишь добавил бы JOIN на users и projects
    // и тянул бы хеши паролей. Что N+1 нет, проверяет QueryCountIntegrationTest.
    Page<Task> findAllByProjectId(Long projectId, Pageable pageable);
}
