package com.mendel.transactions.repository;

import com.mendel.transactions.domain.Transaction;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class InMemoryTransactionRepository implements TransactionRepository {

    @Override
    public Optional<Transaction> save(Transaction transaction) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Optional<Transaction> findById(long id) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public List<Long> findIdsByType(String type) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public List<Long> findChildrenIds(long parentId) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
