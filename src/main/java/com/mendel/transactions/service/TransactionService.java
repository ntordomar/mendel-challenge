package com.mendel.transactions.service;

import com.mendel.transactions.domain.Transaction;
import com.mendel.transactions.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class TransactionService {

    private final TransactionRepository repository;

    public TransactionService(TransactionRepository repository) {
        this.repository = repository;
    }

    public SaveResult save(Transaction transaction) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public List<Long> findIdsByType(String type) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public BigDecimal sum(long id) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
