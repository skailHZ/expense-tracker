package com.example.expense_tracker.mapper;

import com.example.expense_tracker.dto.ExpenseDto;
import com.example.expense_tracker.entity.Expense;
import com.example.expense_tracker.entity.Project;
import com.example.expense_tracker.entity.User;
import java.math.BigDecimal;
import java.time.Instant;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-09-25T23:24:44+0300",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 21.0.12 (Eclipse Adoptium)"
)
@Component
public class ExpenseMapperImpl implements ExpenseMapper {

    @Override
    public ExpenseDto toDto(Expense expense) {
        if ( expense == null ) {
            return null;
        }

        Long projectId = null;
        Long employeeId = null;
        Long id = null;
        BigDecimal amount = null;
        String description = null;
        Instant createdAt = null;

        projectId = expenseProjectId( expense );
        employeeId = expenseEmployeeId( expense );
        id = expense.getId();
        amount = expense.getAmount();
        description = expense.getDescription();
        createdAt = expense.getCreatedAt();

        ExpenseDto expenseDto = new ExpenseDto( id, amount, description, projectId, employeeId, createdAt );

        return expenseDto;
    }

    private Long expenseProjectId(Expense expense) {
        if ( expense == null ) {
            return null;
        }
        Project project = expense.getProject();
        if ( project == null ) {
            return null;
        }
        Long id = project.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }

    private Long expenseEmployeeId(Expense expense) {
        if ( expense == null ) {
            return null;
        }
        User employee = expense.getEmployee();
        if ( employee == null ) {
            return null;
        }
        Long id = employee.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }
}
