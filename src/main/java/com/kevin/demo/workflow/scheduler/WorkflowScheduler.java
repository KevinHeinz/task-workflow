package com.kevin.demo.workflow.scheduler;

import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.kevin.demo.workflow.domain.Task;
import com.kevin.demo.workflow.domain.TaskState;
import com.kevin.demo.workflow.repository.TaskRepository;
import com.kevin.demo.workflow.service.TaskService;

@Component
public class WorkflowScheduler {

    private static final Logger log =
            LoggerFactory.getLogger(WorkflowScheduler.class);

    private final TaskRepository taskRepository;
    private final TaskService taskService;

    public WorkflowScheduler(TaskRepository taskRepository, TaskService taskService) {
        this.taskRepository = taskRepository;
        this.taskService = taskService;
    }

    // check QUEUED tasks and attempt to claim them for PROCESSING
    @Scheduled(fixedDelay = 3_000)
    public void pollQueuedTasks() {

        List<Task> queuedTasks =
                taskRepository.findTop50ByStateOrderByUpdatedAtAsc(TaskState.QUEUED);

        if (queuedTasks.isEmpty()) {
            log.debug("poll_queued empty");

            return;
        }

        int claimedCount = 0;
        log.debug("poll_queued start count={}", queuedTasks.size());

        for (Task task : queuedTasks) {
            try {
                boolean claimed = taskService.claimForProcessing(task.getId());
                if (claimed) {
                    claimedCount++;
                    log.info("task_claimed taskId={}", task.getId());
                }
            } catch (IllegalStateException e) {
                // Expected occasionally (races / invalid state by the time we touch it)
                log.warn("task_claim_skip taskId={} reason={}", task.getId(), e.getMessage());
            } catch (Exception e) {
                // Unexpected
                log.error("task_claim_error taskId={}", task.getId(), e);
            }
        }

        log.info("poll_queued end scanned={} claimed={}", queuedTasks.size(), claimedCount);
    }

    // check PROCESSING tasks and simulate fail first attempt, then complete
    @Scheduled(fixedDelay = 1_500_000)
    public void pollProcessingTasks() {

        List<Task> processing =
                taskRepository.findTop50ByStateOrderByUpdatedAtAsc(TaskState.PROCESSING);

        if (processing.isEmpty()) {
            log.debug("poll_processing empty");
            return;
        }

        int failedCount = 0;
        int completedCount = 0;
        log.debug("poll_processing start count={}", processing.size());

        for (Task task : processing) {
            try {
                // Simulate: first attempt fails, next attempt completes
                boolean shouldFail = task.getAttemptCount() == 0;

                if (shouldFail) {
                    taskService.fail(task.getId(), "Simulated failure on first attempt");
                    failedCount++;
                    log.info("task_failed taskId={} attempt={}", task.getId(), task.getAttemptCount());
                } else {
                    taskService.complete(task.getId());
                    completedCount++;
                    log.info("task_completed taskId={} attempt={}", task.getId(), task.getAttemptCount());
                }
            } catch (IllegalStateException e) {
                log.warn("task_processing_skip taskId={} reason={}", task.getId(), e.getMessage());
            } catch (Exception e) {
                log.error("task_processing_error taskId={}", task.getId(), e);
            }
        }

        log.info("poll_processing end scanned={} failed={} completed={}",
                processing.size(), failedCount, completedCount);
    }

    // check FAILED tasks and auto-retry with backoff (15s/attempt, cap 60s) up to maxAttempts
    @Scheduled(fixedDelay = 5_000)
    public void pollFailedTasks() {

        List<Task> failed =
                taskRepository.findTop50ByStateOrderByUpdatedAtAsc(TaskState.FAILED);

        if (failed.isEmpty()) {
            log.debug("poll_failed empty");
            return;
        }

        int retriedCount = 0;
        int waitingCount = 0;
        log.debug("poll_failed start count={}", failed.size());

        for (Task task : failed) {
            try {
                if (task.getAttemptCount() >= task.getMaxAttempts()) {
                    log.warn("task_retry_giveup taskId={} attempt={} maxAttempts={}",
                            task.getId(), task.getAttemptCount(), task.getMaxAttempts());
                    continue;
                }

                if (!readyToRetry(task)) {
                    waitingCount++;
                    log.debug("task_retry_wait taskId={} attempt={} nextDelaySec={}",
                            task.getId(), task.getAttemptCount(), computeDelaySeconds(task.getAttemptCount()));
                    continue;
                }

                taskService.retry(task.getId());
                retriedCount++;
                log.info("task_retried taskId={} attempt={}", task.getId(), task.getAttemptCount());
            } catch (IllegalStateException e) {
                log.warn("task_retry_skip taskId={} reason={}", task.getId(), e.getMessage());
            } catch (Exception e) {
                log.error("task_retry_error taskId={}", task.getId(), e);
            }
        }

        log.info("poll_failed end scanned={} retried={} waiting={}",
                failed.size(), retriedCount, waitingCount);
    }

    private boolean readyToRetry(Task task) {
        int delaySeconds = computeDelaySeconds(task.getAttemptCount());
        return task.getUpdatedAt()
                .plusSeconds(delaySeconds)
                .isBefore(Instant.now());
    }

    private int computeDelaySeconds(int attemptCount) {
        // 15 seconds per attempt, max 60 seconds
        return Math.min(attemptCount * 15, 60);
    }

    @Scheduled(fixedDelay = 10_000)
    public void pollStaleProcessingTasks() {

        // handles PROCESSING tasks as stale if not updated for 10 minutes
        Instant cutoff = Instant.now().minusSeconds(600);

        List<Task> processing =
                taskRepository.findTop50ByStateOrderByUpdatedAtAsc(TaskState.PROCESSING);

        for (Task task : processing) {

            // stops when hits a non-stale task due to ascending order
            if (!task.getUpdatedAt().isBefore(cutoff)) {
                break;
            }

            try {
                taskService.timeoutProcessingIfStale(task.getId(), cutoff);
            } catch (Exception e) {
                log.warn("task_timeout_error taskId={}", task.getId(), e);
            }
        }
    }
}
