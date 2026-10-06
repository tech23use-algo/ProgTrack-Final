package com.openspace.skillplanner.api;

import com.openspace.skillplanner.api.dto.ApiErrorResponse;
import com.openspace.skillplanner.exception.ApiExceptions.GenerationRateLimitException;
import com.openspace.skillplanner.exception.ApiExceptions.PlanGenerationFailedException;
import com.openspace.skillplanner.exception.ApiExceptions.PlanNotFoundException;
import com.openspace.skillplanner.exception.ApiExceptions.SkillNotAcceptedException;
import com.openspace.skillplanner.exception.ApiExceptions.TaskNotFoundException;
import com.openspace.skillplanner.exception.ApiExceptions.UpstreamAuthenticationException;
import com.openspace.skillplanner.exception.ApiExceptions.UpstreamTimeoutException;
import com.openspace.skillplanner.exception.ApiExceptions.UpstreamUnavailableException;
import jakarta.validation.ConstraintViolationException;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .distinct()
                .collect(Collectors.joining("; "));
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(ConstraintViolationException exception) {
        String message = exception.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .collect(Collectors.joining("; "));
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleUnreadableBody() {
        return error(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "Request body is missing or contains invalid JSON.");
    }

    @ExceptionHandler({PlanNotFoundException.class, TaskNotFoundException.class})
    public ResponseEntity<ApiErrorResponse> handleNotFound(RuntimeException exception) {
        String code = exception instanceof PlanNotFoundException ? "PLAN_NOT_FOUND" : "TASK_NOT_FOUND";
        return error(HttpStatus.NOT_FOUND, code, exception.getMessage());
    }

    @ExceptionHandler(SkillNotAcceptedException.class)
    public ResponseEntity<ApiErrorResponse> handleRejectedSkill(SkillNotAcceptedException exception) {
        return error(HttpStatus.UNPROCESSABLE_ENTITY, "SKILL_NOT_ACCEPTED", exception.getMessage());
    }

    @ExceptionHandler(GenerationRateLimitException.class)
    public ResponseEntity<ApiErrorResponse> handleGenerationLimit(GenerationRateLimitException exception) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, Long.toString(exception.getRetryAfterSeconds()))
                .body(new ApiErrorResponse("GENERATION_RATE_LIMITED", exception.getMessage()));
    }

    @ExceptionHandler(UpstreamUnavailableException.class)
    public ResponseEntity<ApiErrorResponse> handleUpstreamUnavailable(UpstreamUnavailableException exception) {
        ResponseEntity.BodyBuilder response = ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE);
        if (exception.getRetryAfterSeconds() != null) {
            response.header(HttpHeaders.RETRY_AFTER, Long.toString(exception.getRetryAfterSeconds()));
        }
        return response.body(new ApiErrorResponse("AI_PROVIDER_RATE_LIMITED", exception.getMessage()));
    }

    @ExceptionHandler(UpstreamTimeoutException.class)
    public ResponseEntity<ApiErrorResponse> handleUpstreamTimeout(UpstreamTimeoutException exception) {
        return error(HttpStatus.GATEWAY_TIMEOUT, "AI_PROVIDER_TIMEOUT", exception.getMessage());
    }

    @ExceptionHandler({UpstreamAuthenticationException.class, PlanGenerationFailedException.class})
    public ResponseEntity<ApiErrorResponse> handleGenerationFailure(RuntimeException exception) {
        String code = exception instanceof UpstreamAuthenticationException
                ? "AI_PROVIDER_AUTHENTICATION_FAILED"
                : "PLAN_GENERATION_FAILED";
        return error(HttpStatus.BAD_GATEWAY, code, exception.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataIntegrityFailure() {
        return error(HttpStatus.CONFLICT, "DATA_CONFLICT", "The requested change conflicts with stored data.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception exception) {
        logger.error("Unhandled API error type={}", exception.getClass().getSimpleName());
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "An unexpected server error occurred.");
    }

    private ResponseEntity<ApiErrorResponse> error(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(new ApiErrorResponse(code, message));
    }
}
