package com.example.expense_tracker.mapper;

import com.example.expense_tracker.dto.ExpenseCreateDto;
import com.example.expense_tracker.dto.ExpenseDto;
import com.example.expense_tracker.entity.Expense;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ExpenseMapper {

    @Mapping(source = "project.id", target = "projectId")
    @Mapping(source = "employee.id", target = "employeeId")
    ExpenseDto toDto(Expense expense);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "project", ignore = true)
    @Mapping(target = "employee", ignore = true)
    Expense toEntity(ExpenseCreateDto dto);
}