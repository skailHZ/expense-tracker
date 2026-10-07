package com.example.expense_tracker.service;

import com.example.expense_tracker.dto.CurrencyTotalDto;
import com.example.expense_tracker.dto.EmployeeSummaryDto;
import com.example.expense_tracker.dto.StatusSummaryDto;
import com.example.expense_tracker.entity.enums.ExpenseStatus;
import com.example.expense_tracker.repository.ExpenseRepository;
import com.example.expense_tracker.security.ProjectAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExpenseAnalyticsService {

    private final ExpenseRepository expenseRepository;
    private final ProjectAccessService accessService;

    // Затраты проекта по валютам. Отклоненные и отмененные расходы в сумму не входят
    public List<CurrencyTotalDto> totalsByCurrency(Long projectId, String username) {
        accessService.checkAccess(projectId, username);
        return expenseRepository.totalsByCurrency(projectId, ExpenseStatus.COUNTED_IN_TOTALS);
    }

    public List<StatusSummaryDto> summaryByStatus(Long projectId, String username) {
        accessService.checkAccess(projectId, username);
        // Порядок статусов - по жизненному циклу (порядок объявления enum), а не по алфавиту строки в БД
        return expenseRepository.summarizeByStatus(projectId).stream()
                .sorted(Comparator.comparing(StatusSummaryDto::status).thenComparing(StatusSummaryDto::currency))
                .toList();
    }

    // Аналитика по сотрудникам раскрывает траты коллег, поэтому доступна только админу проекта
    public List<EmployeeSummaryDto> summaryByEmployee(Long projectId, Collection<ExpenseStatus> statuses, String username) {
        accessService.checkProjectAdmin(projectId, username);
        Collection<ExpenseStatus> effective = (statuses == null || statuses.isEmpty())
                ? EnumSet.allOf(ExpenseStatus.class)
                : statuses;
        return expenseRepository.summarizeByEmployee(projectId, effective);
    }
}
