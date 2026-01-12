package com.kevin.demo.workflow.api;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.kevin.demo.workflow.api.dto.CreateTaskRequest;
import com.kevin.demo.workflow.api.dto.FailTaskRequest;
import com.kevin.demo.workflow.api.dto.TaskResponse;
import com.kevin.demo.workflow.api.mapper.TaskMapper;
import com.kevin.demo.workflow.repository.TaskRepository;
import com.kevin.demo.workflow.repository.TaskTransitionRepository;
import com.kevin.demo.workflow.service.TaskService;

@RestController
@RequestMapping("api/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService,
                          TaskRepository taskRepository,
                          TaskTransitionRepository transitionRepository) {
        this.taskService = taskService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TaskResponse create(@RequestBody CreateTaskRequest req) {
        return TaskMapper.toResponse(
            taskService.createTask(req.title(), req.description())
        );
    }

    @GetMapping("/{id}")
    public TaskResponse get(@PathVariable Long id) {
        return TaskMapper.toResponse(taskService.get(id));
    }

    @PostMapping("/{id}/submit")
    public TaskResponse submit(@PathVariable Long id) {
        return TaskMapper.toResponse(taskService.submit(id));
    }

    @PostMapping("/{id}/claim")
    public TaskResponse claim(@PathVariable Long id) {
        boolean claimed = taskService.claimForProcessing(id);

        if (!claimed) {
            throw new IllegalStateException(
                    "Task already claimed or not in QUEUED state: " + id
            );
        }

        return TaskMapper.toResponse(taskService.get(id));
    }

    @PostMapping("/{id}/complete")
    public TaskResponse complete(@PathVariable Long id) {
        return TaskMapper.toResponse(taskService.complete(id));
    }

    @PostMapping("/{id}/fail")
    public TaskResponse fail(@PathVariable Long id,
                            @RequestBody FailTaskRequest req) {
        return TaskMapper.toResponse(
                taskService.fail(id, req.message())
        );
    }

    @PostMapping("/{id}/retry")
    public TaskResponse retry(@PathVariable Long id) {
        return TaskMapper.toResponse(taskService.retry(id));
    }
}
