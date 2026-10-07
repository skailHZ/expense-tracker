package com.example.expense_tracker.controller;

import com.example.expense_tracker.dto.ExpenseCreateDto;
import com.example.expense_tracker.dto.ExpenseDto;
import com.example.expense_tracker.service.ExpenseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/expenses")
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseService expenseService;

    @GetMapping
    public ResponseEntity<Page<ExpenseDto>> getExpensesByProject(
            @PathVariable Long projectId,
            Pageable pageable,
            Principal principal) {
        return ResponseEntity.ok(expenseService.getExpensesByProjectId(projectId, pageable, principal.getName()));
    }

    @PostMapping
    public ResponseEntity<ExpenseDto> createExpense(
            @PathVariable Long projectId,
            @Valid @RequestBody ExpenseCreateDto dto,
            Principal principal
    ) {
        ExpenseDto createdExpense = expenseService.createExpense(projectId, dto, principal.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(createdExpense);
    }

    // Аналитический эндпоинт
    @GetMapping("/total")
    public ResponseEntity<Map<String, BigDecimal>> getTotalExpenses(
            @PathVariable Long projectId,
            Principal principal) {
        BigDecimal total = expenseService.calculateTotalProjectExpenses(projectId, principal.getName());
        // Оборачиваем число в Map, чтобы клиент получил валидный JSON: {"totalAmount": 150.50}
        return ResponseEntity.ok(Map.of("totalAmount", total));
    }
}