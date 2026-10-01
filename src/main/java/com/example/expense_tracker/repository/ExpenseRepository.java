package com.example.expense_tracker.repository;

import com.example.expense_tracker.entity.Expense;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    @EntityGraph(attributePaths = {"project", "employee"})
    Page<Expense> findAllByProjectId(Long projectId, Pageable pageable);

    // Агрегация выполняется движком PostgreSQL, а не в памяти Java. Это индустриальный стандарт.
    @Query("SELECT SUM(e.amount) FROM Expense e WHERE e.project.id = :projectId")
    Optional<BigDecimal> calculateTotalAmountByProjectId(@Param("projectId") Long projectId);
}