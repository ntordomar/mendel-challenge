package com.mendel.transactions.service;

import com.mendel.transactions.domain.CircularTransactionException;
import com.mendel.transactions.domain.ParentTransactionNotFoundException;
import com.mendel.transactions.domain.Transaction;
import com.mendel.transactions.domain.TransactionNotFoundException;
import com.mendel.transactions.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Service
public class TransactionService {

    private final TransactionRepository repository;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);

    public TransactionService(TransactionRepository repository) {
        this.repository = repository;
    }

    public SaveResult save(Transaction transaction) {
        lock.writeLock().lock();
        try {
            rejectCyclesAndMissingParents(transaction);
            boolean created = repository.save(transaction).isEmpty();
            return created ? SaveResult.CREATED : SaveResult.UPDATED;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public List<Long> findIdsByType(String type) {
        lock.readLock().lock();
        try {
            return repository.findIdsByType(type);
        } finally {
            lock.readLock().unlock();
        }
    }

    public BigDecimal sumWithDescendants(long id) {
        lock.readLock().lock();
        try {
            List<Transaction> subtree = repository.findWithDescendants(id);
            if (subtree.isEmpty()) {
                throw new TransactionNotFoundException(id);
            }
            return subtree.stream()
                    .map(Transaction::amount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        } finally {
            lock.readLock().unlock();
        }
    }

    private void rejectCyclesAndMissingParents(Transaction transaction) {
        Long parentId = transaction.parentId();
        if (parentId == null) {
            return;
        }
        if (parentId == transaction.id()) {
            throw new CircularTransactionException(transaction.id());
        }
        List<Transaction> ancestors = repository.findWithAncestors(parentId);
        if (ancestors.isEmpty()) {
            throw new ParentTransactionNotFoundException(parentId);
        }
        boolean cycle = ancestors.stream().anyMatch(ancestor -> ancestor.id() == transaction.id());
        if (cycle) {
            throw new CircularTransactionException(transaction.id());
        }
    }
}
