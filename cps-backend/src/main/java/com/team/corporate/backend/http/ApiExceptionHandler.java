package com.team.corporate.backend.http;

import com.team.corporate.backend.dto.ApiErrorDto;
import com.team.corporate.backend.dto.ApiErrorDto.FieldViolation;
import com.team.corporate.core.exceptions.*;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
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

    @ExceptionHandler({HttpMessageNotReadableException.class, TypeMismatchException.class})
    ResponseEntity<ApiErrorDto> malformed(Exception error, HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST, "Некорректный JSON или тип параметра запроса.", request, List.of());
    }

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ApiErrorDto> api(ApiException error, HttpServletRequest request) {
        return response(error.status(), error.getMessage(), request, List.of());
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
        LOG.error("HTTP request failed: {} {}", request.getMethod(), request.getRequestURI(), error);
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "Не удалось выполнить операцию.", request, List.of());
    }

    private ResponseEntity<ApiErrorDto> response(HttpStatus status, String message,
                                                 HttpServletRequest request, List<FieldViolation> violations) {
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON).body(new ApiErrorDto(Instant.now(), status.value(),
                status.getReasonPhrase(), message, request.getRequestURI(), violations));
    }
}
