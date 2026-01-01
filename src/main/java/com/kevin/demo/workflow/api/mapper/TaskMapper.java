package com.kevin.demo.workflow.api.mapper;

import com.kevin.demo.workflow.api.dto.TaskResponse;
import com.kevin.demo.workflow.domain.Task;

public class TaskMapper {

    private TaskMapper() {}

    public static TaskResponse toResponse(Task task) {
        return new TaskResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getState().name(),
                task.getAttemptCount(),
                task.getLastError(),
                task.getMaxAttempts(),
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }
}
