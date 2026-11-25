package com.mendel.transactions.service;

import com.mendel.transactions.domain.CircularTransactionException;
import com.mendel.transactions.domain.ParentTransactionNotFoundException;
import com.mendel.transactions.domain.Transaction;
import com.mendel.transactions.domain.TransactionNotFoundException;
import com.mendel.transactions.repository.InMemoryTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransactionServiceTest {

    private TransactionService service;

    @BeforeEach
    void setUp() {
        service = new TransactionService(new InMemoryTransactionRepository());
    }

    @Test
    void savingNewTransactionReturnsCreated() {
        assertThat(service.save(transaction(10L, "5000", "cars", null))).isEqualTo(SaveResult.CREATED);
    }

    @Test
    void savingExistingTransactionReturnsUpdated() {
        service.save(transaction(10L, "5000", "cars", null));

        assertThat(service.save(transaction(10L, "7000", "cars", null))).isEqualTo(SaveResult.UPDATED);
    }

    @Test
    void savingExistingTransactionReplacesItsAmount() {
        service.save(transaction(10L, "5000", "cars", null));

        service.save(transaction(10L, "7000", "cars", null));

        assertThat(service.sumWithDescendants(10L)).isEqualByComparingTo("7000");
    }

    @Test
    void savingChildOfExistingParentReturnsCreated() {
        service.save(transaction(10L, "5000", "cars", null));

        assertThat(service.save(transaction(11L, "10000", "shopping", 10L))).isEqualTo(SaveResult.CREATED);
    }

    @Test
    void savingWithoutParentClearsThePreviousParent() {
        service.save(transaction(10L, "5000", "cars", null));
        service.save(transaction(11L, "10000", "shopping", 10L));
        service.save(transaction(12L, "5000", "shopping", 11L));

        service.save(transaction(11L, "10000", "shopping", null));

        assertThat(service.sumWithDescendants(10L)).isEqualByComparingTo("5000");
        assertThat(service.sumWithDescendants(11L)).isEqualByComparingTo("15000");
    }

    @Test
    void rejectsParentThatDoesNotExist() {
        assertThatThrownBy(() -> service.save(transaction(11L, "10000", "shopping", 99L)))
                .isInstanceOf(ParentTransactionNotFoundException.class);
    }

    @Test
    void doesNotStoreTransactionWhenParentDoesNotExist() {
        assertThatThrownBy(() -> service.save(transaction(11L, "10000", "shopping", 99L)))
                .isInstanceOf(ParentTransactionNotFoundException.class);

        assertThatThrownBy(() -> service.sumWithDescendants(11L)).isInstanceOf(TransactionNotFoundException.class);
    }

    @Test
    void rejectsTransactionThatIsItsOwnParent() {
        assertThatThrownBy(() -> service.save(transaction(10L, "5000", "cars", 10L)))
                .isInstanceOf(CircularTransactionException.class);
    }

    @Test
    void rejectsDirectCycle() {
        service.save(transaction(10L, "5000", "cars", null));
        service.save(transaction(11L, "10000", "shopping", 10L));

        assertThatThrownBy(() -> service.save(transaction(10L, "5000", "cars", 11L)))
                .isInstanceOf(CircularTransactionException.class);
        assertThat(service.sumWithDescendants(10L)).isEqualByComparingTo("15000");
    }

    @Test
    void rejectsCycleThroughADescendant() {
        service.save(transaction(10L, "5000", "cars", null));
        service.save(transaction(11L, "10000", "shopping", 10L));
        service.save(transaction(12L, "5000", "shopping", 11L));

        assertThatThrownBy(() -> service.save(transaction(10L, "5000", "cars", 12L)))
                .isInstanceOf(CircularTransactionException.class);
        assertThat(service.sumWithDescendants(10L)).isEqualByComparingTo("20000");
    }

    @Test
    void sumsTransactionAndEveryDescendant() {
        service.save(transaction(10L, "5000", "cars", null));
        service.save(transaction(11L, "10000", "shopping", 10L));
        service.save(transaction(12L, "5000", "shopping", 11L));

        assertThat(service.sumWithDescendants(10L)).isEqualByComparingTo("20000");
        assertThat(service.sumWithDescendants(11L)).isEqualByComparingTo("15000");
        assertThat(service.sumWithDescendants(12L)).isEqualByComparingTo("5000");
    }

    @Test
    void sumsEachBranchSeparately() {
        service.save(transaction(10L, "5000", "cars", null));
        service.save(transaction(11L, "10000", "shopping", 10L));
        service.save(transaction(12L, "3000", "shopping", 10L));
        service.save(transaction(13L, "1000", "food", 11L));

        assertThat(service.sumWithDescendants(10L)).isEqualByComparingTo("19000");
        assertThat(service.sumWithDescendants(11L)).isEqualByComparingTo("11000");
        assertThat(service.sumWithDescendants(12L)).isEqualByComparingTo("3000");
    }

    @Test
    void sumOfUnknownTransactionThrows() {
        assertThatThrownBy(() -> service.sumWithDescendants(99L)).isInstanceOf(TransactionNotFoundException.class);
    }

    @Test
    void findsIdsByType() {
        service.save(transaction(10L, "5000", "cars", null));
        service.save(transaction(11L, "10000", "shopping", 10L));
        service.save(transaction(12L, "5000", "shopping", 11L));

        assertThat(service.findIdsByType("cars")).containsExactly(10L);
        assertThat(service.findIdsByType("shopping")).containsExactlyInAnyOrder(11L, 12L);
    }

    @Test
    void findIdsByTypeReturnsEmptyForUnknownType() {
        assertThat(service.findIdsByType("unknown")).isEmpty();
    }

    @Test
    void changingTypeMovesTheTransaction() {
        service.save(transaction(10L, "5000", "cars", null));

        service.save(transaction(10L, "5000", "shopping", null));

        assertThat(service.findIdsByType("cars")).isEmpty();
        assertThat(service.findIdsByType("shopping")).containsExactly(10L);
    }

    private static Transaction transaction(long id, String amount, String type, Long parentId) {
        return new Transaction(id, new BigDecimal(amount), type, parentId);
    }
}
