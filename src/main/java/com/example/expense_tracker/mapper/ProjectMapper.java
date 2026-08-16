package com.example.expense_tracker.mapper;

import com.example.expense_tracker.dto.ProjectDto;
import com.example.expense_tracker.entity.Project;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

// componentModel = "spring" заставляет MapStruct сгенерировать @Component класс,
// чтобы мы могли инжектить этот маппер через @RequiredArgsConstructor
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ProjectMapper {

    // MapStruct сам сообразит, как замаппить примитивы, но нам нужно указать,
    // откуда брать adminId, так как в сущности Project лежит целый объект User admin.
    @Mapping(source = "admin.id", target = "adminId")
    ProjectDto toDto(Project project);
}