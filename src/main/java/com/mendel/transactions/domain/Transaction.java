package com.mendel.transactions.domain;

import java.math.BigDecimal;

/**
 * A transaction of a given type and amount, optionally linked to a parent transaction.
 *
 * @param parentId id of the parent transaction, or {@code null} if it has no parent
 */
public record Transaction(long id, BigDecimal amount, String type, Long parentId) {
}
