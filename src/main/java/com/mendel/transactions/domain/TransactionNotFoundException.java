package com.mendel.transactions.domain;

public class TransactionNotFoundException extends RuntimeException {

    public TransactionNotFoundException(long id) {
        super("Transaction not found: " + id);
    }
}
