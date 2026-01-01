package com.kevin.demo.workflow.scheduler;

import java.time.Instant;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.kevin.demo.workflow.domain.Task;
import com.kevin.demo.workflow.domain.TaskState;
import com.kevin.demo.workflow.repository.TaskRepository;
import com.kevin.demo.workflow.service.TaskService;

@Component
public class WorkflowScheduler {

    private final TaskRepository taskRepository;
    private final TaskService taskService;

    public WorkflowScheduler(TaskRepository taskRepository, TaskService taskService) {
        this.taskRepository = taskRepository;
        this.taskService = taskService;
    }

    // checks every 3 seconds for QUEUED tasks to start PROCESSING 
    @Scheduled(fixedDelay = 3000)
    public void pollQueuedTasks() {

        List<Task> queuedTasks =
                taskRepository.findTop50ByStateOrderByUpdatedAtAsc(TaskState.QUEUED);

        for (Task task : queuedTasks) {
            try {
                boolean claimed = taskService.claimForProcessing(task.getId());
                if (claimed) {
                    System.out.println("[scheduler] claimed task id=" + task.getId());
                }
            } catch (Exception e) {
                System.out.println(
                        "[scheduler] skip task id=" + task.getId()
                        + " reason=" + e.getMessage()
                );
            }
        }
    }

    // checks every 3 seconds for PROCESSING tasks. Simulates FAILED, retries, then COMPLETED  
    @Scheduled(fixedDelay = 3000)
    public void pollProcessingTasks() {

        List<Task> processing =
                taskRepository.findTop50ByStateOrderByUpdatedAtAsc(TaskState.PROCESSING);

        for (Task task : processing) {
            try {
                // for observation of fail path execution
                boolean shouldFail = task.getAttemptCount() == 0;

                if (shouldFail) {
                    taskService.fail(task.getId(), "Simulated failure on first attempt");
                    System.out.println("[scheduler] failed task id=" + task.getId());
                } else {
                    taskService.complete(task.getId());
                    System.out.println("[scheduler] completed task id=" + task.getId());
                }
            } catch (Exception e) {
                System.out.println(
                        "[scheduler] processing-skip task id=" + task.getId()
                        + " reason=" + e.getMessage()
                );
            }
        }
    }

    // auto-retry FAILED tasks with linear backoff (15s/attempt, cap 60s) up to maxAttempts.
    @Scheduled(fixedDelay = 5_000)
    public void pollFailedTasks() {
        List<Task> failed =
            taskRepository.findTop50ByStateOrderByUpdatedAtAsc(TaskState.FAILED);

        for (Task task : failed) {
            try {
                if (task.getAttemptCount() >= task.getMaxAttempts()) {
                    continue; // give up
                }

                if (!readyToRetry(task)) {
                    continue; // wait longer
                }

                taskService.retry(task.getId());
                System.out.println("[scheduler] auto-retry task id=" + task.getId());

            } catch (Exception e) {
                System.out.println("[scheduler] retry-skip task id=" + task.getId()
                        + " reason=" + e.getMessage());
            }
        }
    }

    private boolean readyToRetry(Task task) {
        int attempt = task.getAttemptCount();

        // 15 seconds per attempt, max 60 seconds
        int delaySeconds = Math.min(attempt * 15, 60);

        return task.getUpdatedAt()
                .plusSeconds(delaySeconds)
                .isBefore(Instant.now());
    }
}
