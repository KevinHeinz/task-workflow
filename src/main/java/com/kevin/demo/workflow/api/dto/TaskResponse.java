package com.kevin.demo.workflow.api.dto;

import java.time.Instant;

public record TaskResponse(
        Long id,
        String title,
        String description,
        String state,
        int attemptCount,
        String lastError,
        int maxAttempts,
        Instant createdAt,
        Instant updatedAt
) {}
