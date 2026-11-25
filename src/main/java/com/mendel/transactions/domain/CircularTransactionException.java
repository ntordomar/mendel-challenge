package com.mendel.transactions.domain;

public class CircularTransactionException extends RuntimeException {

    public CircularTransactionException(long id) {
        super("Transaction " + id + " would create a cycle");
    }
}
