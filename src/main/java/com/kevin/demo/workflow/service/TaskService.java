package com.kevin.demo.workflow.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kevin.demo.workflow.domain.Task;
import com.kevin.demo.workflow.domain.TaskState;
import com.kevin.demo.workflow.domain.TaskTransition;
import com.kevin.demo.workflow.repository.TaskRepository;
import com.kevin.demo.workflow.repository.TaskTransitionRepository;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final TaskTransitionRepository transitionRepository;

    public TaskService(TaskRepository taskRepository,
                       TaskTransitionRepository transitionRepository) {
        this.taskRepository = taskRepository;
        this.transitionRepository = transitionRepository;
    }

    // create task
    @Transactional
    public Task createTask(String title, String description) {
        Task task = new Task(title, description);
        Task saved = taskRepository.save(task);

        transitionRepository.save(
            new TaskTransition(
                saved,
                null,
                saved.getState(),
                "CREATE",
                "Task created"
            )
        );

        return saved;
    }

    // transition: created -> queued 
    @Transactional
    public Task submit(Long taskId) {
        Task task = taskRepository.findById(taskId)
            .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

        requireState(task, TaskState.CREATED, "SUBMIT");

        transitionRepository.save(
            new TaskTransition(
                task, 
                task.getState(), 
                TaskState.QUEUED, 
                "SUBMIT", 
                null
            )
        );

        task.setState(TaskState.QUEUED);
        return taskRepository.save(task);
    }

    // transition queued -> processing
    @Transactional
    public Task startProcessing(Long taskId) {
        Task task = taskRepository.findById(taskId)
            .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

        requireState(task, TaskState.QUEUED, "START_PROCESSING");

        transitionRepository.save(
                new TaskTransition(
                        task,
                        task.getState(),
                        TaskState.PROCESSING,
                        "START_PROCESSING",
                        null
                )
        );

        task.setState(TaskState.PROCESSING);
        return taskRepository.save(task);
    }

    // transition: processing -> completed
    @Transactional
    public Task complete(Long taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

        requireState(task, TaskState.PROCESSING, "COMPLETE");

        transitionRepository.save(
                new TaskTransition(
                        task,
                        task.getState(),
                        TaskState.COMPLETED,
                        "COMPLETE",
                        null
                )
        );

        task.setState(TaskState.COMPLETED);
        return taskRepository.save(task);
    }

    // transition: processing -> failed
    @Transactional
    public Task fail(Long taskId, String errorMessage) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

        requireState(task, TaskState.PROCESSING, "FAIL");

        task.setAttemptCount(task.getAttemptCount() + 1);
        task.setLastError(errorMessage);

        transitionRepository.save(
                new TaskTransition(
                        task,
                        task.getState(),
                        TaskState.FAILED,
                        "FAIL",
                        errorMessage
                )
        );

        task.setState(TaskState.FAILED);
        return taskRepository.save(task);
    }

    // transition: failed -> queued (retry) 
    @Transactional
    public Task retry(Long taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

        requireState(task, TaskState.FAILED, "RETRY");

        if (task.getAttemptCount() >= task.getMaxAttempts()) {
            throw new IllegalStateException("Max attempts reached for task: " + taskId);
        }

        transitionRepository.save(
                new TaskTransition(
                        task,
                        task.getState(),
                        TaskState.QUEUED,
                        "RETRY",
                        null
                )
        );

        task.setState(TaskState.QUEUED);
        return taskRepository.save(task);
    }

    private void requireState(Task task, TaskState expected, String action) {
        if (task.getState() != expected) {
            throw new IllegalStateException(
                    "Invalid transition: action=" + action +
                    " expected=" + expected +
                    " actual=" + task.getState() +
                    " taskId=" + task.getId()
            );
        }
    }
}
