package com.kevin.demo.workflow.service;

import java.time.Instant;

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

        TaskState from = task.getState();

        task.setState(TaskState.QUEUED);
        Task saved = taskRepository.save(task);

        recordTransitionIfChanged(saved, from, "SUBMIT", null);

        return saved;
    }

    // transition: queued -> processing 
    @Transactional
    public boolean claimForProcessing(Long taskId) {

        int updated = taskRepository.transitionStateIfCurrent(
                taskId,
                TaskState.QUEUED,
                TaskState.PROCESSING
        );

        if (updated == 0) return false;

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

        transitionRepository.save(
                new TaskTransition(
                        task,
                        TaskState.QUEUED,
                        TaskState.PROCESSING,
                        "START_PROCESSING",
                        null
                )
        );

        return true;
    }

    // transition: processing -> completed
    @Transactional
    public Task complete(Long taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

        requireState(task, TaskState.PROCESSING, "COMPLETE");

        TaskState from = task.getState();

        task.setState(TaskState.COMPLETED);
        Task saved = taskRepository.save(task);

        recordTransitionIfChanged(saved, from, "COMPLETE", null);

        return saved;
    }

    // transition: processing -> failed
    @Transactional
    public Task fail(Long taskId, String errorMessage) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

        requireState(task, TaskState.PROCESSING, "FAIL");

        TaskState from = task.getState();

        task.setAttemptCount(task.getAttemptCount() + 1);
        task.setLastError(errorMessage);
        task.setState(TaskState.FAILED);

        Task saved = taskRepository.save(task);

        recordTransitionIfChanged(saved, from, "FAIL", errorMessage);

        return saved;
    }

    // transition: after failed -> queued (again) for retry 
    @Transactional
    public Task retry(Long taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

        if (task.getState() == TaskState.QUEUED
                || task.getState() == TaskState.PROCESSING
                || task.getState() == TaskState.COMPLETED) {
            return task;
        }

        requireState(task, TaskState.FAILED, "RETRY");

        if (task.getAttemptCount() >= task.getMaxAttempts()) {
            throw new IllegalStateException("Max attempts reached for task: " + taskId);
        }

        TaskState from = task.getState();

        task.setState(TaskState.QUEUED);
        task.setLastError(null);
        Task saved = taskRepository.save(task);

        recordTransitionIfChanged(saved, from, "RETRY", null);

        return saved;
    }

    @Transactional(readOnly = true)
    public Task get(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + id));
    }

    private void recordTransitionIfChanged(
            Task task,
            TaskState fromState,
            String event,
            String message
    ) {
        if (fromState != task.getState()) {
            transitionRepository.save(
                    new TaskTransition(
                            task,
                            fromState,
                            task.getState(),
                            event,
                            message
                    )
            );
        }
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

    @Transactional
    public boolean timeoutProcessingIfStale(Long taskId, Instant cutoff) {

        int updated = taskRepository.transitionStateIfCurrentAndStale(
                taskId,
                TaskState.PROCESSING,
                TaskState.FAILED,
                cutoff,
                "PROCESSING_TIMEOUT"
        );

        if (updated == 0) return false;

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

        transitionRepository.save(
                new TaskTransition(
                        task,
                        TaskState.PROCESSING,
                        TaskState.FAILED,
                        "TIMEOUT",
                        null
                )
        );

        return true;
    }
}
