package com.example.expense_tracker.service;

import com.example.expense_tracker.dto.TaskDto;
import com.example.expense_tracker.mapper.TaskMapper;
import com.example.expense_tracker.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final TaskMapper taskMapper;

    @Transactional(readOnly = true)
    public Page<TaskDto> getTasksByProjectId(Long projectId, Pageable pageable) {
        // Метод Page.map() позволяет преобразовать Page<Task> в Page<TaskDto>,
        // сохраняя всю мета-информацию пагинации (totalElements, totalPages)
        return taskRepository.findAllByProjectId(projectId, pageable)
                .map(taskMapper::toDto);
    }
}