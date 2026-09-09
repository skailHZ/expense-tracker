package com.example.expense_tracker.mapper;

import com.example.expense_tracker.dto.ProjectCreateDto;
import com.example.expense_tracker.dto.ProjectDto;
import com.example.expense_tracker.entity.Project;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ProjectMapper {

    @Mapping(source = "admin.id", target = "adminId")
    ProjectDto toDto(Project project);

    // Игнорируем поля, которые генерируются базой данных или устанавливаются в сервисе
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "admin", ignore = true)
    Project toEntity(ProjectCreateDto dto);
}