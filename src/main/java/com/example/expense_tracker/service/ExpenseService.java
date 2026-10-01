package com.example.expense_tracker.service;

import com.example.expense_tracker.dto.ExpenseCreateDto;
import com.example.expense_tracker.dto.ExpenseDto;
import com.example.expense_tracker.entity.Expense;
import com.example.expense_tracker.entity.Project;
import com.example.expense_tracker.entity.User;
import com.example.expense_tracker.exception.ResourceNotFoundException;
import com.example.expense_tracker.mapper.ExpenseMapper;
import com.example.expense_tracker.repository.ExpenseRepository;
import com.example.expense_tracker.repository.ProjectRepository;
import com.example.expense_tracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ExpenseMapper expenseMapper;

    @Transactional(readOnly = true)
    public Page<ExpenseDto> getExpensesByProjectId(Long projectId, Pageable pageable) {
        if (!projectRepository.existsById(projectId)) {
            throw new ResourceNotFoundException("Project not found: " + projectId);
        }
        return expenseRepository.findAllByProjectId(projectId, pageable)
                .map(expenseMapper::toDto);
    }

    @Transactional
    public ExpenseDto createExpense(Long projectId, ExpenseCreateDto dto, String username) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found: " + projectId));

        User employee = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

        Expense expense = expenseMapper.toEntity(dto);
        expense.setProject(project);
        expense.setEmployee(employee);

        Expense savedExpense = expenseRepository.save(expense);
        return expenseMapper.toDto(savedExpense);
    }

    @Transactional(readOnly = true)
    public BigDecimal calculateTotalProjectExpenses(Long projectId) {
        if (!projectRepository.existsById(projectId)) {
            throw new ResourceNotFoundException("Project not found: " + projectId);
        }
        // Если расходов еще нет, SUM() вернет null. Обрабатываем это и возвращаем 0.00
        return expenseRepository.calculateTotalAmountByProjectId(projectId)
                .orElse(BigDecimal.ZERO);
    }
}