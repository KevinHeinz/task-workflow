package com.kevin.demo.workflow.domain;

import java.time.Instant;

import jakarta.persistence.*;

@Entity
@Table(name = "tasks")
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // optimistic locking version prevents conflicts in concurrent updates
    @Version
    private Long version;

    @Enumerated(jakarta.persistence.EnumType.STRING)
    @Column(nullable = false)
    private TaskState state = TaskState.CREATED;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private int attemptCount = 0;

    @Column(nullable = false)
    private int maxAttempts = 5;

    @Column(columnDefinition = "TEXT")
    private String lastError;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    public Task() {}

    public Task(String title, String description) {
        this.title = title;
        this.description = description;
        this.state = TaskState.CREATED;
    }

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = Instant.now();
    }

    // Getters and setters  
    public Long getId() {
        return id;
    }
    public Long getVersion() {
        return version;
    }

    public TaskState getState() {
        return state;
    }
    public void setState(TaskState state) {
        this.state = state;
    }

    public String getTitle() {
        return title;
    }
    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }

    public int getAttemptCount() {
        return attemptCount;
    }
    public void setAttemptCount(int attemptCount) {
        this.attemptCount = attemptCount;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }
    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public String getLastError() {
        return lastError;
    }
    public void setLastError(String lastError) {
        this.lastError = lastError;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }   
    public Instant getUpdatedAt() {
        return updatedAt;
    }
}