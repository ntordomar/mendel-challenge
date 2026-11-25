package com.mendel.transactions.repository;

import com.mendel.transactions.domain.Transaction;
import org.springframework.stereotype.Repository;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Repository
public class InMemoryTransactionRepository implements TransactionRepository {

    private final Map<Long, Transaction> transactionsById = new HashMap<>();
    private final Map<String, Set<Long>> idsByType = new HashMap<>();
    private final Map<Long, Set<Long>> childrenByParentId = new HashMap<>();

    @Override
    public Optional<Transaction> save(Transaction transaction) {
        Transaction previous = transactionsById.put(transaction.id(), transaction);
        if (previous != null) {
            removeFromIndex(idsByType, previous.type(), previous.id());
            if (previous.parentId() != null) {
                removeFromIndex(childrenByParentId, previous.parentId(), previous.id());
            }
        }
        addToIndex(idsByType, transaction.type(), transaction.id());
        if (transaction.parentId() != null) {
            addToIndex(childrenByParentId, transaction.parentId(), transaction.id());
        }
        return Optional.ofNullable(previous);
    }

    @Override
    public Optional<Transaction> findById(long id) {
        return Optional.ofNullable(transactionsById.get(id));
    }

    @Override
    public List<Long> findIdsByType(String type) {
        Set<Long> ids = idsByType.get(type);
        return ids == null ? List.of() : List.copyOf(ids);
    }

    @Override
    public List<Transaction> findWithDescendants(long id) {
        if (!transactionsById.containsKey(id)) {
            return List.of();
        }
        List<Transaction> subtree = new ArrayList<>();
        ArrayDeque<Long> pending = new ArrayDeque<>();
        pending.add(id);
        while (!pending.isEmpty()) {
            long currentId = pending.remove();
            subtree.add(transactionsById.get(currentId));
            Set<Long> children = childrenByParentId.get(currentId);
            if (children != null) {
                pending.addAll(children);
            }
        }
        return List.copyOf(subtree);
    }

    @Override
    public List<Transaction> findWithAncestors(long id) {
        Transaction current = transactionsById.get(id);
        if (current == null) {
            return List.of();
        }
        List<Transaction> ancestors = new ArrayList<>();
        while (current != null) {
            ancestors.add(current);
            Long parentId = current.parentId();
            current = parentId == null ? null : transactionsById.get(parentId);
        }
        return List.copyOf(ancestors);
    }

    private <K> void addToIndex(Map<K, Set<Long>> index, K key, long id) {
        index.computeIfAbsent(key, ignored -> new HashSet<>()).add(id);
    }

    private <K> void removeFromIndex(Map<K, Set<Long>> index, K key, long id) {
        Set<Long> ids = index.get(key);
        if (ids == null) {
            return;
        }
        ids.remove(id);
        if (ids.isEmpty()) {
            index.remove(key);
        }
    }
}
