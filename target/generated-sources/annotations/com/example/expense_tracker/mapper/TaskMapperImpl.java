package com.example.expense_tracker.mapper;

import com.example.expense_tracker.dto.TaskCreateDto;
import com.example.expense_tracker.dto.TaskDto;
import com.example.expense_tracker.entity.Project;
import com.example.expense_tracker.entity.Task;
import com.example.expense_tracker.entity.User;
import com.example.expense_tracker.entity.enums.TaskStatus;
import java.time.Instant;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-09-25T23:24:45+0300",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 21.0.12 (Eclipse Adoptium)"
)
@Component
public class TaskMapperImpl implements TaskMapper {

    @Override
    public TaskDto toDto(Task task) {
        if ( task == null ) {
            return null;
        }

        Long projectId = null;
        Long employeeId = null;
        Long id = null;
        String title = null;
        TaskStatus status = null;
        Instant createdAt = null;

        projectId = taskProjectId( task );
        employeeId = taskEmployeeId( task );
        id = task.getId();
        title = task.getTitle();
        status = task.getStatus();
        createdAt = task.getCreatedAt();

        TaskDto taskDto = new TaskDto( id, title, status, projectId, employeeId, createdAt );

        return taskDto;
    }

    @Override
    public Task toEntity(TaskCreateDto dto) {
        if ( dto == null ) {
            return null;
        }

        Task task = new Task();

        task.setTitle( dto.title() );
        task.setStatus( dto.status() );

        return task;
    }

    private Long taskProjectId(Task task) {
        if ( task == null ) {
            return null;
        }
        Project project = task.getProject();
        if ( project == null ) {
            return null;
        }
        Long id = project.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }

    private Long taskEmployeeId(Task task) {
        if ( task == null ) {
            return null;
        }
        User employee = task.getEmployee();
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
