package com.example.expense_tracker.controller;

import com.example.expense_tracker.dto.ChangeExpenseStatusRequest;
import com.example.expense_tracker.dto.CurrencyTotalDto;
import com.example.expense_tracker.dto.EmployeeSummaryDto;
import com.example.expense_tracker.dto.ExpenseCreateDto;
import com.example.expense_tracker.dto.ExpenseCreationResult;
import com.example.expense_tracker.dto.ExpenseDto;
import com.example.expense_tracker.dto.ExpenseStatusChangeDto;
import com.example.expense_tracker.dto.StatusSummaryDto;
import com.example.expense_tracker.entity.enums.ExpenseStatus;
import com.example.expense_tracker.exception.BadRequestException;
import com.example.expense_tracker.service.ExpenseAnalyticsService;
import com.example.expense_tracker.service.ExpenseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/expenses")
@RequiredArgsConstructor
public class ExpenseController {

    static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";
    static final String IDEMPOTENT_REPLAYED_HEADER = "Idempotent-Replayed";
    private static final int MAX_IDEMPOTENCY_KEY_LENGTH = 255;

    private final ExpenseService expenseService;
    private final ExpenseAnalyticsService analyticsService;

    @GetMapping
    public ResponseEntity<Page<ExpenseDto>> getExpensesByProject(
            @PathVariable Long projectId,
            @RequestParam(required = false) ExpenseStatus status,
            Pageable pageable,
            Principal principal) {
        return ResponseEntity.ok(expenseService.getExpensesByProjectId(projectId, status, pageable, principal.getName()));
    }

    /**
     * Заголовок Idempotency-Key необязателен, но рекомендуется: клиент генерирует уникальный ключ (UUID)
     * на каждую логическую операцию и безопасно повторяет запрос при сетевых сбоях.
     * При повторе возвращается тот же расход и заголовок Idempotent-Replayed: true.
     */
    @PostMapping
    public ResponseEntity<ExpenseDto> createExpense(
            @PathVariable Long projectId,
            @Valid @RequestBody ExpenseCreateDto dto,
            @RequestHeader(value = IDEMPOTENCY_KEY_HEADER, required = false) String idempotencyKey,
            Principal principal
    ) {
        ExpenseCreationResult result = expenseService.createExpense(
                projectId, dto, principal.getName(), normalizeKey(idempotencyKey));

        ResponseEntity.BodyBuilder response = ResponseEntity.status(HttpStatus.CREATED);
        if (result.replayed()) {
            response.header(IDEMPOTENT_REPLAYED_HEADER, "true");
        }
        return response.body(result.expense());
    }

    // Перевод расхода по жизненному циклу: PENDING -> APPROVED/REJECTED/CANCELLED, APPROVED -> PAID/REJECTED
    @PostMapping("/{expenseId}/status")
    public ResponseEntity<ExpenseDto> changeStatus(
            @PathVariable Long projectId,
            @PathVariable Long expenseId,
            @Valid @RequestBody ChangeExpenseStatusRequest request,
            Principal principal
    ) {
        return ResponseEntity.ok(expenseService.changeStatus(projectId, expenseId, request, principal.getName()));
    }

    @GetMapping("/{expenseId}/history")
    public ResponseEntity<List<ExpenseStatusChangeDto>> getHistory(
            @PathVariable Long projectId,
            @PathVariable Long expenseId,
            Principal principal
    ) {
        return ResponseEntity.ok(expenseService.getHistory(projectId, expenseId, principal.getName()));
    }

    // Затраты по валютам (без отклоненных и отмененных): [{"currency":"RUB","amount":150.50}, ...]
    @GetMapping("/total")
    public ResponseEntity<List<CurrencyTotalDto>> getTotalExpenses(
            @PathVariable Long projectId,
            Principal principal) {
        return ResponseEntity.ok(analyticsService.totalsByCurrency(projectId, principal.getName()));
    }

    @GetMapping("/analytics/by-status")
    public ResponseEntity<List<StatusSummaryDto>> getSummaryByStatus(
            @PathVariable Long projectId,
            Principal principal) {
        return ResponseEntity.ok(analyticsService.summaryByStatus(projectId, principal.getName()));
    }

    // Только админ проекта. Фильтр: ?status=APPROVED&status=PAID (без фильтра - все статусы)
    @GetMapping("/analytics/by-employee")
    public ResponseEntity<List<EmployeeSummaryDto>> getSummaryByEmployee(
            @PathVariable Long projectId,
            @RequestParam(name = "status", required = false) List<ExpenseStatus> statuses,
            Principal principal) {
        return ResponseEntity.ok(analyticsService.summaryByEmployee(projectId, statuses, principal.getName()));
    }

    private String normalizeKey(String rawKey) {
        if (rawKey == null) {
            return null;
        }
        String key = rawKey.trim();
        if (key.isEmpty() || key.length() > MAX_IDEMPOTENCY_KEY_LENGTH) {
            throw new BadRequestException(IDEMPOTENCY_KEY_HEADER + " must be 1-" + MAX_IDEMPOTENCY_KEY_LENGTH + " characters");
        }
        return key;
    }
}
