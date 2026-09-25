package com.example.expense_tracker.mapper;

import com.example.expense_tracker.dto.TaskCreateDto;
import com.example.expense_tracker.dto.TaskDto;
import com.example.expense_tracker.entity.Task;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface TaskMapper {

    @Mapping(source = "project.id", target = "projectId")
    @Mapping(source = "employee.id", target = "employeeId")
    TaskDto toDto(Task task);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "project", ignore = true)
    @Mapping(target = "employee", ignore = true)
    Task toEntity(TaskCreateDto dto);
}