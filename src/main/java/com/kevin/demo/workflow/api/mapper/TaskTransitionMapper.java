package com.kevin.demo.workflow.api.mapper;

import com.kevin.demo.workflow.api.dto.TaskTransitionResponse;
import com.kevin.demo.workflow.domain.TaskTransition;

public class TaskTransitionMapper {

    private TaskTransitionMapper() {}

    public static TaskTransitionResponse toResponse(TaskTransition t) {
        return new TaskTransitionResponse(
                t.getId(),
                t.getFromState() == null ? null : t.getFromState().name(),
                t.getToState() == null ? null : t.getToState().name(),
                t.getEvent(),
                t.getMessage(),
                t.getCreatedAt()
        );
    }
}
