package com.kevin.demo.workflow.api;

import com.kevin.demo.workflow.api.dto.TaskTransitionResponse;
import com.kevin.demo.workflow.api.mapper.TaskTransitionMapper;
import com.kevin.demo.workflow.domain.TaskTransition;
import com.kevin.demo.workflow.repository.TaskTransitionRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks/{taskId}/transitions")
public class TaskTransitionController {

    private final TaskTransitionRepository transitionRepository;

    public TaskTransitionController(TaskTransitionRepository transitionRepository) {
        this.transitionRepository = transitionRepository;
    }

    @GetMapping
    public List<TaskTransitionResponse> list(@PathVariable Long taskId) {

        List<TaskTransition> transitions =
                transitionRepository.findByTaskIdOrderByCreatedAtAsc(taskId);

        return transitions.stream()
                .map(TaskTransitionMapper::toResponse)
                .toList();
    }
}
