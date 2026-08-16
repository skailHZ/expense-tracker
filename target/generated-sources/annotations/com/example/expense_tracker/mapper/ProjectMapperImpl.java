package com.example.expense_tracker.mapper;

import com.example.expense_tracker.dto.ProjectDto;
import com.example.expense_tracker.entity.Project;
import com.example.expense_tracker.entity.User;
import java.time.Instant;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-08-16T07:27:07+0300",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 21.0.12 (Eclipse Adoptium)"
)
@Component
public class ProjectMapperImpl implements ProjectMapper {

    @Override
    public ProjectDto toDto(Project project) {
        if ( project == null ) {
            return null;
        }

        Long adminId = null;
        Long id = null;
        String name = null;
        String description = null;
        Instant createdAt = null;

        adminId = projectAdminId( project );
        id = project.getId();
        name = project.getName();
        description = project.getDescription();
        createdAt = project.getCreatedAt();

        ProjectDto projectDto = new ProjectDto( id, name, description, adminId, createdAt );

        return projectDto;
    }

    private Long projectAdminId(Project project) {
        if ( project == null ) {
            return null;
        }
        User admin = project.getAdmin();
        if ( admin == null ) {
            return null;
        }
        Long id = admin.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }
}
