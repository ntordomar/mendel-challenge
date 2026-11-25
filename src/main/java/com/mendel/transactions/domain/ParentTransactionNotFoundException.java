package com.mendel.transactions.domain;

public class ParentTransactionNotFoundException extends RuntimeException {

    public ParentTransactionNotFoundException(long parentId) {
        super("Parent transaction not found: " + parentId);
    }
}
