package com.kevin.demo.workflow.repository;

import java.util.List;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import com.kevin.demo.workflow.domain.Task;
import com.kevin.demo.workflow.domain.TaskState;

public interface TaskRepository extends JpaRepository<Task, Long>{
    
    // Used by TaskScheduler to find work
    List<Task> findTop50ByStateOrderByUpdatedAtAsc(TaskState state);

    @Modifying
    @Query("update Task t set t.state = :toState where t.id = :id and t.state = :fromState")
    int transitionStateIfCurrent(
        @Param("id") Long id,
        @Param("fromState") TaskState fromState,
        @Param("toState") TaskState toState
    );
}
