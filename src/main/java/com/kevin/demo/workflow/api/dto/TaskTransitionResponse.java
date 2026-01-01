package com.kevin.demo.workflow.api.dto;

import java.time.Instant;

public record TaskTransitionResponse(
        Long id,
        String fromState,
        String toState,
        String event,
        String message,
        Instant createdAt
) {}