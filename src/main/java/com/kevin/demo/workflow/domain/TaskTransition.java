package com.kevin.demo.workflow.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "task_transitions")
public class TaskTransition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskState fromState;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskState toState;

    @Column(nullable = false)
    private String event;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(nullable = false)
    private Instant createdAt;

    public TaskTransition() {}

    public TaskTransition(
            Task task,
            TaskState fromState,
            TaskState toState,
            String event,
            String message
    ) {
        this.task = task;
        this.fromState = fromState;
        this.toState = toState;
        this.event = event;
        this.message = message;
    }

    @PrePersist
    void prePersist() {
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public Task getTask() { return task; }
    public TaskState getFromState() { return fromState; }
    public TaskState getToState() { return toState; }
    public String getEvent() { return event; }
    public String getMessage() { return message; }
    public Instant getCreatedAt() { return createdAt; }
}
