package com.mendel.transactions.web;

import com.mendel.transactions.domain.CircularTransactionException;
import com.mendel.transactions.domain.ParentTransactionNotFoundException;
import com.mendel.transactions.domain.TransactionNotFoundException;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.webmvc.error.ErrorAttributes;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class TransactionExceptionHandler {

    private final ErrorAttributes errorAttributes;

    public TransactionExceptionHandler(ErrorAttributes errorAttributes) {
        this.errorAttributes = errorAttributes;
    }

    @ExceptionHandler(TransactionNotFoundException.class)
    public ResponseEntity<Map<String, Object>> transactionNotFound(
            TransactionNotFoundException exception, HttpServletRequest request) {
        return body(HttpStatus.NOT_FOUND, request, exception.getMessage());
    }

    @ExceptionHandler(ParentTransactionNotFoundException.class)
    public ResponseEntity<Map<String, Object>> parentNotFound(
            ParentTransactionNotFoundException exception, HttpServletRequest request) {
        return body(HttpStatus.UNPROCESSABLE_CONTENT, request, exception.getMessage());
    }

    @ExceptionHandler(CircularTransactionException.class)
    public ResponseEntity<Map<String, Object>> cycle(
            CircularTransactionException exception, HttpServletRequest request) {
        return body(HttpStatus.UNPROCESSABLE_CONTENT, request, exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> invalid(
            MethodArgumentNotValidException exception, HttpServletRequest request) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(this::fieldMessage)
                .collect(Collectors.joining(", "));
        return body(HttpStatus.BAD_REQUEST, request, message);
    }

    private String fieldMessage(FieldError error) {
        return error.getField() + " " + error.getDefaultMessage();
    }

    private ResponseEntity<Map<String, Object>> body(HttpStatus status, HttpServletRequest request, String message) {
        request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, status.value());
        request.setAttribute(RequestDispatcher.ERROR_REQUEST_URI, request.getRequestURI());
        Map<String, Object> body = new LinkedHashMap<>(errorAttributes.getErrorAttributes(
                new ServletWebRequest(request), ErrorAttributeOptions.defaults()));
        body.put("message", message);
        return ResponseEntity.status(status).body(body);
    }
}
