package com.sigi.services.exception;

import com.sigi.presentation.dto.response.ApiResponse;
import io.jsonwebtoken.JwtException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.NoHandlerFoundException;

import static com.sigi.util.Constants.*;

import java.util.List;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private ResponseEntity<ApiResponse<Void>> buildErrorResponse(HttpStatus status, String message, List<String> errors) {
        return ResponseEntity.status(status).body(
                ApiResponse.error(status.value(), message, errors)
        );
    }

    @ExceptionHandler({NoHandlerFoundException.class, EntityNotFoundException.class})
    public ResponseEntity<ApiResponse<Void>> handleNotFound(Exception ex) {
        log.error("Resource not found. type={}, message={}", ex.getClass().getSimpleName(), ex.getMessage());

        List<String> errors = switch (ex) {
            case NoHandlerFoundException e -> List.of("No handler found for this URL: " + e.getRequestURL());
            case EntityNotFoundException e -> List.of(e.getMessage());
            default -> List.of("Resource not found.");
        };
        return buildErrorResponse(HttpStatus.NOT_FOUND, RESOURCE_NOT_FOUND, errors);
    }

    @ExceptionHandler({AccessDeniedException.class, JwtException.class, AuthenticationException.class, ResponseStatusException.class})
    public ResponseEntity<ApiResponse<Void>> handleAuthExceptions(Exception ex) {
        HttpStatus status;
        String message;

        switch (ex) {
            case AccessDeniedException e -> {
                status = HttpStatus.FORBIDDEN;
                message = ACCESS_DENIED;
            }
            case JwtException e -> {
                status = HttpStatus.UNAUTHORIZED;
                message = INVALID_TOKEN;
            }
            case AuthenticationException e -> {
                status = HttpStatus.UNAUTHORIZED;
                message = AUTHENTICATION_FAILED;
            }
            case ResponseStatusException e -> {
                status = (HttpStatus) e.getStatusCode();
                message = e.getReason() != null ? e.getReason() : AUTH_ERROR;
            }
            default -> {
                status = HttpStatus.UNAUTHORIZED;
                message = "Authentication error";
            }
        }

        log.warn("Authentication/Authorization error. type={}, message={}, status={}}", ex.getClass().getSimpleName(), ex.getMessage(), status);

        return buildErrorResponse(status, message, List.of(ex.getMessage()));
    }

    @ExceptionHandler({ValidationException.class, MethodArgumentNotValidException.class, ConstraintViolationException.class})
    public ResponseEntity<ApiResponse<Void>> handleValidationExceptions(Exception ex) {
        log.warn("Validation error. type={}, message={}", ex.getClass().getSimpleName(), ex.getMessage());
        List<String> errors = switch (ex) {
            case ConstraintViolationException e -> e.getConstraintViolations()
                    .stream()
                    .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                    .toList();

            case ValidationException e -> List.of(e.getMessage());

            case MethodArgumentNotValidException e -> e.getBindingResult()
                    .getAllErrors()
                    .stream()
                    .map(DefaultMessageSourceResolvable::getDefaultMessage)
                    .toList();

            default -> List.of("Validation error.");
        };

        return buildErrorResponse(HttpStatus.BAD_REQUEST, VALIDATION_ERROR, errors);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataConflict(DataIntegrityViolationException ex) {
        log.error("Data conflict detected. cause={}", ex.getMostSpecificCause().getMessage());
        return buildErrorResponse(HttpStatus.CONFLICT, DATA_CONFLICT, List.of(ex.getMostSpecificCause().getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        log.error("Unexpected error occurred. type={}, message={}", ex.getClass().getSimpleName(), ex.getMessage(), ex);
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR, List.of(ex.getMessage()));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        log.error("handle Method Not Supported. cause={}", ex.getMessage());
        return buildErrorResponse(HttpStatus.METHOD_NOT_ALLOWED, METHOD_NOT_ALLOWED, List.of(ex.getMessage()));
    }
}
