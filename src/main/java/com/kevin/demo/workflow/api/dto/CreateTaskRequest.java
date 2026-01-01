package com.kevin.demo.workflow.api.dto;

public record CreateTaskRequest(
    String title,
    String description
) {}
