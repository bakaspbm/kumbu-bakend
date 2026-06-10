package com.kumbu.backend.exception;



import jakarta.validation.ConstraintViolation;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;

import org.springframework.http.converter.HttpMessageNotReadableException;

import org.springframework.validation.FieldError;

import org.springframework.web.bind.MethodArgumentNotValidException;

import org.springframework.web.bind.annotation.ExceptionHandler;

import org.springframework.web.bind.annotation.RestControllerAdvice;

import org.springframework.web.method.annotation.HandlerMethodValidationException;



import java.time.Instant;

import java.util.HashMap;

import java.util.Map;

import java.util.stream.Collectors;



@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {



    @ExceptionHandler(ApiException.class)

    public ResponseEntity<Map<String, Object>> handleApiException(ApiException ex) {

        Map<String, Object> body = errorBody(ex.getCode(), ex.getMessage());
        if (ex instanceof ProfileIncompleteException profileEx) {
            body.put("missing_fields", profileEx.getMissingFields());
        }
        return ResponseEntity.status(ex.getStatus()).body(body);

    }



    @ExceptionHandler(MethodArgumentNotValidException.class)

    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {

        Map<String, String> fields = new HashMap<>();

        for (FieldError error : ex.getBindingResult().getFieldErrors()) {

            fields.put(error.getField(), error.getDefaultMessage());

        }

        Map<String, Object> body = errorBody("VALIDATION_ERROR", "Dados inválidos");

        body.put("fields", fields);

        return ResponseEntity.badRequest().body(body);

    }



    @ExceptionHandler(HandlerMethodValidationException.class)

    public ResponseEntity<Map<String, Object>> handleHandlerMethodValidation(HandlerMethodValidationException ex) {

        Map<String, String> fields = new HashMap<>();

        ex.getAllValidationResults().forEach(result ->

                result.getResolvableErrors().forEach(error -> {

                    String field = error.getCodes() != null && error.getCodes().length > 0

                            ? error.getCodes()[0]

                            : "parameter";

                    fields.put(field, error.getDefaultMessage());

                })

        );

        Map<String, Object> body = errorBody("VALIDATION_ERROR", "Parâmetros inválidos");

        body.put("fields", fields);

        return ResponseEntity.badRequest().body(body);

    }



    @ExceptionHandler(ConstraintViolationException.class)

    public ResponseEntity<Map<String, Object>> handleConstraintViolation(ConstraintViolationException ex) {

        Map<String, String> fields = ex.getConstraintViolations().stream()

                .collect(Collectors.toMap(

                        v -> fieldName(v),

                        ConstraintViolation::getMessage,

                        (a, b) -> b

                ));

        Map<String, Object> body = errorBody("VALIDATION_ERROR", "Parâmetros inválidos");

        body.put("fields", fields);

        return ResponseEntity.badRequest().body(body);

    }



    @ExceptionHandler(HttpMessageNotReadableException.class)

    public ResponseEntity<Map<String, Object>> handleUnreadable(HttpMessageNotReadableException ex) {

        return ResponseEntity.badRequest()

                .body(errorBody("VALIDATION_ERROR", "Corpo da requisição inválido ou mal formatado"));

    }



    @ExceptionHandler(IllegalArgumentException.class)

    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {

        return ResponseEntity.badRequest()

                .body(errorBody("VALIDATION_ERROR", ex.getMessage() != null ? ex.getMessage() : "Valor inválido"));

    }



    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> handleAuthentication(AuthenticationException ex) {
        return ResponseEntity.status(401)
                .body(errorBody("UNAUTHORIZED", "Email ou palavra-passe incorrectos"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity.internalServerError()

                .body(errorBody("INTERNAL_ERROR", "Erro interno do servidor"));

    }



    private static String fieldName(ConstraintViolation<?> violation) {

        String path = violation.getPropertyPath().toString();

        int dot = path.lastIndexOf('.');

        return dot >= 0 ? path.substring(dot + 1) : path;

    }



    private Map<String, Object> errorBody(String code, String message) {

        Map<String, Object> body = new HashMap<>();

        body.put("code", code);

        body.put("message", message);

        body.put("timestamp", Instant.now().toString());

        return body;

    }

}

