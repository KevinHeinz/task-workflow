package com.kevin.demo.workflow.scheduler;

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

    // checks every 3 seconds for queued tasks to start processing 
    @Scheduled(fixedDelay = 3000)
    public void pollQueuedTasks() {

        List<Task> queuedTasks =
                taskRepository.findTop50ByStateOrderByUpdatedAtAsc(TaskState.QUEUED);

        for (Task task : queuedTasks) {
            try {
                taskService.startProcessing(task.getId());
                System.out.println("[scheduler] started task id=" + task.getId());
            } catch (Exception e) {
                // Expected if this task was already claimed
                System.out.println(
                        "[scheduler] skip task id=" + task.getId()
                        + " reason=" + e.getMessage()
                );
            }
        }
    }
}
