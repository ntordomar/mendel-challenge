package com.mendel.transactions.repository;

import com.mendel.transactions.domain.Transaction;

import java.util.List;
import java.util.Optional;

public interface TransactionRepository {

    /**
     * Stores the transaction, replacing any existing transaction with the same id.
     *
     * @return the replaced transaction, or empty if none existed with that id
     */
    Optional<Transaction> save(Transaction transaction);

    Optional<Transaction> findById(long id);

    /**
     * @return the ids of all transactions of the given type, or an empty list if there are none
     */
    List<Long> findIdsByType(String type);

    /**
     * Returns the transaction with the given id plus every descendant, read atomically.
     *
     * @return those transactions, or an empty list if no transaction has that id
     */
    List<Transaction> findWithDescendants(long id);
}
