package com.kevin.demo.workflow.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kevin.demo.workflow.domain.TaskTransition;

public interface TaskTransitionRepository extends JpaRepository<TaskTransition, Long> {

    List<TaskTransition> findByTaskIdOrderByCreatedAtAsc(Long taskId);
}
