package com.example.expense_tracker.repository;

import com.example.expense_tracker.dto.CurrencyTotalDto;
import com.example.expense_tracker.dto.EmployeeSummaryDto;
import com.example.expense_tracker.dto.StatusSummaryDto;
import com.example.expense_tracker.entity.Expense;
import com.example.expense_tracker.entity.enums.ExpenseStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    // Без @EntityGraph: ExpenseDto использует только id проекта и сотрудника (см. QueryCountIntegrationTest)
    Page<Expense> findAllByProjectId(Long projectId, Pageable pageable);

    Page<Expense> findAllByProjectIdAndStatus(Long projectId, ExpenseStatus status, Pageable pageable);

    // Расход ищем вместе с проектом: иначе id расхода из чужого проекта обошел бы проверку доступа (BOLA)
    Optional<Expense> findByIdAndProjectId(Long id, Long projectId);

    boolean existsByIdAndProjectId(Long id, Long projectId);

    // Все агрегации выполняет PostgreSQL (GROUP BY), а не Java. Суммы разных валют не смешиваются.
    @Query("""
            SELECT new com.example.expense_tracker.dto.CurrencyTotalDto(e.currency, SUM(e.amount))
            FROM Expense e
            WHERE e.project.id = :projectId AND e.status IN :statuses
            GROUP BY e.currency
            ORDER BY e.currency
            """)
    List<CurrencyTotalDto> totalsByCurrency(@Param("projectId") Long projectId,
                                            @Param("statuses") Collection<ExpenseStatus> statuses);

    @Query("""
            SELECT new com.example.expense_tracker.dto.StatusSummaryDto(e.status, e.currency, COUNT(e), SUM(e.amount))
            FROM Expense e
            WHERE e.project.id = :projectId
            GROUP BY e.status, e.currency
            """)
    List<StatusSummaryDto> summarizeByStatus(@Param("projectId") Long projectId);

    @Query("""
            SELECT new com.example.expense_tracker.dto.EmployeeSummaryDto(u.id, u.username, e.currency, COUNT(e), SUM(e.amount))
            FROM Expense e JOIN e.employee u
            WHERE e.project.id = :projectId AND e.status IN :statuses
            GROUP BY u.id, u.username, e.currency
            ORDER BY u.username, e.currency
            """)
    List<EmployeeSummaryDto> summarizeByEmployee(@Param("projectId") Long projectId,
                                                 @Param("statuses") Collection<ExpenseStatus> statuses);
}
