package com.team.corporate.backend.http;

import com.team.corporate.backend.dto.ApiErrorDto;
import com.team.corporate.backend.dto.ApiErrorDto.FieldViolation;
import com.team.corporate.backend.validation.InvalidUsernameException;
import com.team.corporate.backend.validation.InvalidPasswordException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.access.AccessDeniedException;
import com.team.corporate.backend.security.RateLimitException;
import com.team.corporate.backend.security.SecurityAudit;
import com.team.corporate.core.exceptions.*;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
//import org.springframework.security.access.AccessDeniedException;
//import org.springframework.security.core.AuthenticationException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.sql.SQLException;
import java.time.Instant;
import java.util.List;

@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger LOG = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ApiErrorDto> denied(AccessDeniedException error, HttpServletRequest request) {
        SecurityAudit.record("HTTP_ACCESS", SecurityAudit.currentActor(), null, "DENIED");
        return response(HttpStatus.FORBIDDEN, "Недостаточно прав.", request, List.of());
    }

    @ExceptionHandler(RateLimitException.class)
    ResponseEntity<ApiErrorDto> rateLimited(RateLimitException error, HttpServletRequest request) {
        SecurityAudit.record("AUTH_LIMIT", SecurityAudit.currentActor(), null, "REJECTED");
        var result = response(HttpStatus.TOO_MANY_REQUESTS, error.getMessage(), request, List.of());
        return ResponseEntity.status(429).header("Retry-After", Long.toString(error.retryAfter()))
                .header("Cache-Control", "no-store").contentType(MediaType.APPLICATION_JSON).body(result.getBody());
    }

    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<ApiErrorDto> authentication(AuthenticationException error, HttpServletRequest request) {
        SecurityAudit.record("AUTHENTICATE", SecurityAudit.currentActor(), null, "REJECTED");
        return response(HttpStatus.UNAUTHORIZED, "Недействительные полномочия или сессия.", request, List.of());
    }

    @ExceptionHandler({InvalidUsernameException.class, InvalidPasswordException.class})
    ResponseEntity<ApiErrorDto> invalidInput(IllegalArgumentException error, HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST, error.getMessage(), request, List.of());
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, TypeMismatchException.class})
    ResponseEntity<ApiErrorDto> malformed(Exception error, HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST, "Некорректный JSON или тип параметра запроса.", request, List.of());
    }

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ApiErrorDto> api(ApiException error, HttpServletRequest request) {
        return response(error.status(), error.getMessage(), request, List.of());
    }

    @ExceptionHandler(CsvImportException.class)
    ResponseEntity<ApiErrorDto> csvImport(CsvImportException error, HttpServletRequest request) {
        var status = switch (error.kind()) {
            case INVALID_CSV -> HttpStatus.BAD_REQUEST;
            case CONFLICT -> HttpStatus.CONFLICT;
        };
        return response(status, error.getMessage(), request, List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiErrorDto> validation(MethodArgumentNotValidException error, HttpServletRequest request) {
        var violations = error.getBindingResult().getFieldErrors().stream()
                .map(field -> new FieldViolation(field.getField(), field.getDefaultMessage())).toList();
        return response(HttpStatus.BAD_REQUEST, "Некорректные параметры запроса.", request, violations);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    ResponseEntity<ApiErrorDto> methodValidation(HandlerMethodValidationException error, HttpServletRequest request) {
        var violations = error.getParameterValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream().map(problem ->
                        new FieldViolation(result.getMethodParameter().getParameterName(), problem.getDefaultMessage())))
                .toList();
        return response(HttpStatus.BAD_REQUEST, "Некорректные параметры запроса.", request, violations);
    }

    @ExceptionHandler({CategoryNotFoundException.class, ProductNotFoundException.class,
            WarehouseNotFoundException.class, InventoryNotFoundException.class, UserNotFoundException.class,
            OrderNotFoundException.class, OrderItemNotFoundException.class})
    ResponseEntity<ApiErrorDto> missing(RuntimeException error, HttpServletRequest request) {
        return response(HttpStatus.NOT_FOUND, error.getMessage(), request, List.of());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ApiErrorDto> businessRule(IllegalArgumentException error, HttpServletRequest request) {
        String message = error.getMessage();
        for (Throwable cause = error; cause != null; cause = cause.getCause()) {
            if (cause instanceof SQLException) {
                message = "Операция конфликтует с существующими данными.";
                break;
            }
        }
        return response(HttpStatus.CONFLICT, message, request, List.of());
    }

    // конфликт версий -> 409
    @ExceptionHandler(OptimisticLockingFailureException.class)
    ResponseEntity<ApiErrorDto> versionConflict(OptimisticLockingFailureException error, HttpServletRequest request) {
        return response(HttpStatus.CONFLICT,
                "Запись была изменена другим пользователем. Обновите страницу и повторите действие.",
                request, List.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiErrorDto> dataConflict(DataIntegrityViolationException error, HttpServletRequest request) {
        return response(HttpStatus.CONFLICT, "Операция конфликтует с существующими данными.", request, List.of());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiErrorDto> other(Exception error, HttpServletRequest request) {
        if (error instanceof ErrorResponse frameworkError) {
            var status = HttpStatus.valueOf(frameworkError.getStatusCode().value());
            var response = response(status, status.getReasonPhrase(), request, List.of());
            var headers = new HttpHeaders();
            headers.putAll(frameworkError.getHeaders());
            headers.setContentType(MediaType.APPLICATION_JSON);
            return new ResponseEntity<>(response.getBody(), headers, status);
        }
        // Exception messages/stack traces may contain SQL values or malformed credential JSON.
        LOG.error("HTTP request failed type={} method={} path={} correlationId={}",
                error.getClass().getSimpleName(), request.getMethod(), request.getRequestURI(), correlationId(request));
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "Не удалось выполнить операцию.", request, List.of());
    }

    private ResponseEntity<ApiErrorDto> response(HttpStatus status, String message,
                                                 HttpServletRequest request, List<FieldViolation> violations) {
        String correlationId = correlationId(request);
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON)
                .body(new ApiErrorDto(Instant.now(), status.value(),
                        status.getReasonPhrase(), message, request.getRequestURI(),
                        correlationId, violations));
    }

    private static String correlationId(HttpServletRequest request) {
        if (request == null) return null;
        Object value = request.getAttribute(CorrelationIdFilter.REQUEST_ATTRIBUTE);
        return value == null ? null : value.toString();
    }
}
