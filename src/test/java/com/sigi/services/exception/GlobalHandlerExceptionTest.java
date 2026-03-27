package com.sigi.services.exception;

import com.sigi.presentation.dto.response.ApiResponse;
import io.jsonwebtoken.JwtException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.lang.reflect.Method;
import java.util.Set;

import static com.sigi.util.Constants.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @InjectMocks
    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    // ------------------- handleNotFound -------------------
    @Test
    void shouldHandleNoHandlerFoundException() {
        NoHandlerFoundException ex = new NoHandlerFoundException("GET", "/test", new HttpHeaders());
        ResponseEntity<ApiResponse<Void>> response = handler.handleNotFound(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(RESOURCE_NOT_FOUND, response.getBody().getMessage());
        assertTrue(response.getBody().getErrors().get(0).contains("/test"));
    }

    @Test
    void shouldHandleEntityNotFoundException() {
        EntityNotFoundException ex = new EntityNotFoundException("Entity not found");
        ResponseEntity<ApiResponse<Void>> response = handler.handleNotFound(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(RESOURCE_NOT_FOUND, response.getBody().getMessage());
        assertEquals("Entity not found", response.getBody().getErrors().get(0));
    }

    // ------------------- handleAuthExceptions -------------------
    @Test
    void shouldHandleAccessDeniedException() {
        AccessDeniedException ex = new AccessDeniedException("Forbidden");
        ResponseEntity<ApiResponse<Void>> response = handler.handleAuthExceptions(ex);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals(ACCESS_DENIED, response.getBody().getMessage());
    }

    @Test
    void shouldHandleJwtException() {
        JwtException ex = new JwtException("Invalid token");
        ResponseEntity<ApiResponse<Void>> response = handler.handleAuthExceptions(ex);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals(INVALID_TOKEN, response.getBody().getMessage());
    }

    @Test
    void shouldHandleAuthenticationException() {
        AuthenticationException ex = new AuthenticationException("Auth failed") {};
        ResponseEntity<ApiResponse<Void>> response = handler.handleAuthExceptions(ex);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals(AUTHENTICATION_FAILED, response.getBody().getMessage());
    }

    @Test
    void shouldHandleResponseStatusException() {
        ResponseStatusException ex = new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bad request");
        ResponseEntity<ApiResponse<Void>> response = handler.handleAuthExceptions(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Bad request", response.getBody().getMessage());
    }

    // ------------------- handleValidationExceptions -------------------
    @Test
    void shouldHandleValidationException() {
        ValidationException ex = new ValidationException("Validation failed");
        ResponseEntity<ApiResponse<Void>> response = handler.handleValidationExceptions(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(VALIDATION_ERROR, response.getBody().getMessage());
        assertEquals("Validation failed", response.getBody().getErrors().get(0));
    }

    @Test
    void shouldHandleConstraintViolationException() {
        ConstraintViolationException ex = new ConstraintViolationException(Set.of());
        ResponseEntity<ApiResponse<Void>> response = handler.handleValidationExceptions(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(VALIDATION_ERROR, response.getBody().getMessage());
    }

    class DummyController {
        public void dummyMethod(@Valid String param) {}
    }

    @Test
    void shouldHandleMethodArgumentNotValidException() throws NoSuchMethodException {
        Method method = DummyController.class.getDeclaredMethod("dummyMethod", String.class);
        MethodParameter methodParameter = new MethodParameter(method, 0);
        BindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "object");
        bindingResult.addError(new ObjectError("object", "Invalid field"));
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(methodParameter, bindingResult);

        ResponseEntity<ApiResponse<Void>> response = handler.handleValidationExceptions(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(VALIDATION_ERROR, response.getBody().getMessage());
        assertTrue(response.getBody().getErrors().contains("Invalid field"));
    }

    // ------------------- handleDataConflict -------------------
    @Test
    void shouldHandleDataConflict() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException("Conflict", new Throwable("Duplicate key"));
        ResponseEntity<ApiResponse<Void>> response = handler.handleDataConflict(ex);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals(DATA_CONFLICT, response.getBody().getMessage());
    }

    // ------------------- handleGenericException -------------------
    @Test
    void shouldHandleGenericException() {
        Exception ex = new Exception("Unexpected error");
        ResponseEntity<ApiResponse<Void>> response = handler.handleGenericException(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(INTERNAL_SERVER_ERROR, response.getBody().getMessage());
    }

    // ------------------- handleMethodNotSupported -------------------
    @Test
    void shouldHandleMethodNotSupported() {
        HttpRequestMethodNotSupportedException ex = new HttpRequestMethodNotSupportedException("POST");
        ResponseEntity<ApiResponse<Void>> response = handler.handleMethodNotSupported(ex);

        assertEquals(HttpStatus.METHOD_NOT_ALLOWED, response.getStatusCode());
        assertEquals(METHOD_NOT_ALLOWED, response.getBody().getMessage());
    }
}