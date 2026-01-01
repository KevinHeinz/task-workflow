package com.kevin.demo.workflow.api;

import com.kevin.demo.workflow.api.dto.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(
            IllegalArgumentException ex,
            HttpServletRequest request
    ) {
        // Used for "not found" in service
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiError> handleIllegalState(
            IllegalStateException ex,
            HttpServletRequest request
    ) {
        // Used for invalid transitions / max attempts
        return build(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(
            Exception ex,
            HttpServletRequest request
    ) {
        // fallback
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error", request);
    }

    private ResponseEntity<ApiError> build(
            HttpStatus status,
            String message,
            HttpServletRequest request
    ) {
        ApiError body = new ApiError(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI()
        );
        return ResponseEntity.status(status).body(body);
    }
}
