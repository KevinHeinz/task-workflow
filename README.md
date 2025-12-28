# task-workflow
State-driven task workflow service with retries and worker processing.

## Overview
This service models a task lifecycle using explicit states:

CREATED → QUEUED → PROCESSING → FAILED (retry) → COMPLETED

It enforces valid state transitions, supports retries, and records transition history.
A background worker automatically processes queued tasks.

## Tech Stack
- Java 17
- Spring Boot
- Spring Data JPA
- MySQL
- Maven

## Key Concepts
- Explicit state machine
- Optimistic locking
- Idempotent transitions
- Background worker loop

## Running Locally
```bash
./mvnw spring-boot:run
