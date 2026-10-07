package com.example.expense_tracker.repository;

import com.example.expense_tracker.entity.ExpenseStatusChange;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExpenseStatusChangeRepository extends JpaRepository<ExpenseStatusChange, Long> {

    // changedBy нужен для имени автора изменения, подгружаем одним запросом
    @EntityGraph(attributePaths = "changedBy")
    List<ExpenseStatusChange> findAllByExpenseIdOrderByChangedAtAscIdAsc(Long expenseId);
}
