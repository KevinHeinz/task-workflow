package com.kevin.demo.workflow.api.dto;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.kevin.demo.workflow.domain.Task;
import com.kevin.demo.workflow.domain.TaskTransition;
import com.kevin.demo.workflow.repository.TaskRepository;
import com.kevin.demo.workflow.repository.TaskTransitionRepository;
import com.kevin.demo.workflow.service.TaskService;

@RestController
@RequestMapping("api/tasks")
public class TaskController {

    private final TaskService taskService;
    private final TaskRepository taskRepository;
    private final TaskTransitionRepository transitionRepository;

    public TaskController(TaskService taskService,
                          TaskRepository taskRepository,
                          TaskTransitionRepository transitionRepository) {
        this.taskService = taskService;
        this.taskRepository = taskRepository;
        this.transitionRepository = transitionRepository;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Task create(@RequestBody CreateTaskRequest req) {
        return taskService.createTask(req.title, req.description);
    }

    @GetMapping("/{id}")
    public Task get(@PathVariable Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + id));
    }

    @GetMapping("/{id}/transitions")
    public List<TaskTransition> transitions(@PathVariable Long id) {
        return transitionRepository.findByTaskIdOrderByCreatedAtAsc(id);
    }

    @PostMapping("/{id}/submit")
    public Task submit(@PathVariable Long id) {
        return taskService.submit(id);
    }

    @PostMapping("/{id}/start")
    public Task start(@PathVariable Long id) {
        return taskService.startProcessing(id);
    }

    @PostMapping("/{id}/complete")
    public Task complete(@PathVariable Long id) {
        return taskService.complete(id);
    }

    @PostMapping("/{id}/fail")
    public Task fail(@PathVariable Long id, @RequestBody FailTaskRequest req) {
        return taskService.fail(id, req.message);
    }

    @PostMapping("/{id}/retry")
    public Task retry(@PathVariable Long id) {
        return taskService.retry(id);
    }
}
