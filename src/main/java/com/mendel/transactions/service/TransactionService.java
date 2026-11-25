package com.mendel.transactions.service;

import com.mendel.transactions.domain.CircularTransactionException;
import com.mendel.transactions.domain.ParentTransactionNotFoundException;
import com.mendel.transactions.domain.Transaction;
import com.mendel.transactions.domain.TransactionNotFoundException;
import com.mendel.transactions.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class TransactionService {

    private final TransactionRepository repository;
    private final Object lock = new Object();

    public TransactionService(TransactionRepository repository) {
        this.repository = repository;
    }

    public SaveResult save(Transaction transaction) {
        synchronized (lock) {
            rejectCyclesAndMissingParents(transaction);
            boolean created = repository.save(transaction).isEmpty();
            return created ? SaveResult.CREATED : SaveResult.UPDATED;
        }
    }

    public List<Long> findIdsByType(String type) {
        synchronized (lock) {
            return repository.findIdsByType(type);
        }
    }

    public BigDecimal sumWithDescendants(long id) {
        synchronized (lock) {
            List<Transaction> subtree = repository.findWithDescendants(id);
            if (subtree.isEmpty()) {
                throw new TransactionNotFoundException(id);
            }
            return subtree.stream()
                    .map(Transaction::amount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
    }

    private void rejectCyclesAndMissingParents(Transaction transaction) {
        Long ancestorId = transaction.parentId();
        while (ancestorId != null) {
            if (ancestorId == transaction.id()) {
                throw new CircularTransactionException(transaction.id());
            }
            Transaction ancestor = repository.findById(ancestorId).orElse(null);
            if (ancestor == null) {
                throw new ParentTransactionNotFoundException(transaction.parentId());
            }
            ancestorId = ancestor.parentId();
        }
    }
}
