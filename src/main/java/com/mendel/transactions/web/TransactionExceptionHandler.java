package com.mendel.transactions.web;

import com.mendel.transactions.domain.CircularTransactionException;
import com.mendel.transactions.domain.ParentTransactionNotFoundException;
import com.mendel.transactions.domain.TransactionNotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class TransactionExceptionHandler {

    @ExceptionHandler({TransactionNotFoundException.class, ParentTransactionNotFoundException.class})
    public ResponseEntity<Void> notFound() {
        return ResponseEntity.notFound().build();
    }

    @ExceptionHandler(CircularTransactionException.class)
    public ResponseEntity<Void> cycle() {
        return ResponseEntity.unprocessableContent().build();
    }
}
