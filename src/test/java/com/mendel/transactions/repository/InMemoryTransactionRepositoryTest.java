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
    void findWithDescendantsReturnsEmptyWhenTransactionDoesNotExist() {
        assertThat(repository.findWithDescendants(99L)).isEmpty();
    }

    @Test
    void findWithDescendantsReturnsTheTransactionWhenItHasNoDescendants() {
        Transaction transaction = transaction(10L, "5000", "cars", null);
        repository.save(transaction);

        assertThat(repository.findWithDescendants(10L)).containsExactly(transaction);
    }

    @Test
    void findWithDescendantsReturnsTheTransactionAndEveryDescendant() {
        Transaction root = transaction(10L, "5000", "cars", null);
        Transaction child = transaction(11L, "10000", "shopping", 10L);
        Transaction grandchild = transaction(12L, "5000", "shopping", 11L);
        Transaction otherChild = transaction(13L, "1000", "food", 10L);
        repository.save(root);
        repository.save(child);
        repository.save(grandchild);
        repository.save(otherChild);

        assertThat(repository.findWithDescendants(10L)).containsExactlyInAnyOrder(root, child, grandchild, otherChild);
        assertThat(repository.findWithDescendants(11L)).containsExactlyInAnyOrder(child, grandchild);
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
    void replacingTransactionWithNewParentMovesItsDescendants() {
        Transaction firstParent = transaction(1L, "100", "cars", null);
        Transaction secondParent = transaction(2L, "100", "cars", null);
        repository.save(firstParent);
        repository.save(secondParent);
        repository.save(transaction(10L, "5000", "cars", 1L));

        Transaction moved = transaction(10L, "5000", "cars", 2L);
        repository.save(moved);

        assertThat(repository.findWithDescendants(1L)).containsExactly(firstParent);
        assertThat(repository.findWithDescendants(2L)).containsExactlyInAnyOrder(secondParent, moved);
    }

    @Test
    void replacingTransactionWithoutParentDetachesItFromPreviousParent() {
        Transaction parent = transaction(1L, "100", "cars", null);
        repository.save(parent);
        repository.save(transaction(10L, "5000", "cars", 1L));

        repository.save(transaction(10L, "5000", "cars", null));

        assertThat(repository.findWithDescendants(1L)).containsExactly(parent);
    }

    private static Transaction transaction(long id, String amount, String type, Long parentId) {
        return new Transaction(id, new BigDecimal(amount), type, parentId);
    }
}
