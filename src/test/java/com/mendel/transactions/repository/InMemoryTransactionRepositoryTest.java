package com.mendel.transactions.repository;

import com.mendel.transactions.domain.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryTransactionRepositoryTest {

    private InMemoryTransactionRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryTransactionRepository();
    }

    @Test
    void findsSavedTransactionById() {
        Transaction transaction = transaction(10L, "5000", "cars", null);

        repository.save(transaction);

        assertThat(repository.findById(10L)).contains(transaction);
    }

    @Test
    void returnsEmptyWhenTransactionDoesNotExist() {
        assertThat(repository.findById(99L)).isEmpty();
    }

    @Test
    void savingNewTransactionReturnsEmpty() {
        assertThat(repository.save(transaction(10L, "5000", "cars", null))).isEmpty();
    }

    @Test
    void savingExistingTransactionReturnsTheReplacedOne() {
        Transaction original = transaction(10L, "5000", "cars", null);
        repository.save(original);

        assertThat(repository.save(transaction(10L, "7000", "cars", null))).contains(original);
    }

    @Test
    void findsIdsByType() {
        repository.save(transaction(10L, "5000", "cars", null));
        repository.save(transaction(11L, "10000", "shopping", 10L));
        repository.save(transaction(12L, "5000", "shopping", 11L));

        assertThat(repository.findIdsByType("cars")).containsExactly(10L);
        assertThat(repository.findIdsByType("shopping")).containsExactlyInAnyOrder(11L, 12L);
    }

    @Test
    void returnsNoIdsForUnknownType() {
        assertThat(repository.findIdsByType("unknown")).isEmpty();
    }

    @Test
    void findsDirectChildrenIds() {
        repository.save(transaction(10L, "5000", "cars", null));
        repository.save(transaction(11L, "10000", "shopping", 10L));
        repository.save(transaction(12L, "5000", "shopping", 10L));
        repository.save(transaction(13L, "1000", "food", 11L));

        assertThat(repository.findChildrenIds(10L)).containsExactlyInAnyOrder(11L, 12L);
        assertThat(repository.findChildrenIds(11L)).containsExactly(13L);
    }

    @Test
    void returnsNoChildrenIdsForTransactionWithoutChildren() {
        repository.save(transaction(10L, "5000", "cars", null));

        assertThat(repository.findChildrenIds(10L)).isEmpty();
    }

    @Test
    void replacingTransactionUpdatesItsData() {
        repository.save(transaction(10L, "5000", "cars", null));
        Transaction replacement = transaction(10L, "7000", "cars", null);

        repository.save(replacement);

        assertThat(repository.findById(10L)).contains(replacement);
        assertThat(repository.findIdsByType("cars")).containsExactly(10L);
    }

    @Test
    void replacingTransactionWithNewTypeMovesItToThatType() {
        repository.save(transaction(10L, "5000", "cars", null));

        repository.save(transaction(10L, "5000", "shopping", null));

        assertThat(repository.findIdsByType("cars")).isEmpty();
        assertThat(repository.findIdsByType("shopping")).containsExactly(10L);
    }

    @Test
    void replacingTransactionWithNewParentMovesItToThatParent() {
        repository.save(transaction(1L, "100", "cars", null));
        repository.save(transaction(2L, "100", "cars", null));
        repository.save(transaction(10L, "5000", "cars", 1L));

        repository.save(transaction(10L, "5000", "cars", 2L));

        assertThat(repository.findChildrenIds(1L)).isEmpty();
        assertThat(repository.findChildrenIds(2L)).containsExactly(10L);
    }

    @Test
    void replacingTransactionWithoutParentRemovesItFromPreviousParent() {
        repository.save(transaction(1L, "100", "cars", null));
        repository.save(transaction(10L, "5000", "cars", 1L));

        repository.save(transaction(10L, "5000", "cars", null));

        assertThat(repository.findChildrenIds(1L)).isEmpty();
    }

    private static Transaction transaction(long id, String amount, String type, Long parentId) {
        return new Transaction(id, new BigDecimal(amount), type, parentId);
    }
}
