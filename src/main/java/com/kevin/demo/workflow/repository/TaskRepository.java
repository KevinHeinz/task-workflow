package com.kevin.demo.workflow.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import com.kevin.demo.workflow.domain.Task;
import com.kevin.demo.workflow.domain.TaskState;

public interface TaskRepository extends JpaRepository<Task, Long>{
    
    // Used by TaskScheduler to find work
    List<Task> findTop50ByStateOrderByUpdatedAtAsc(TaskState state);
}
