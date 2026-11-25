package com.mendel.transactions.service;

import com.mendel.transactions.domain.CircularTransactionException;
import com.mendel.transactions.domain.ParentTransactionNotFoundException;
import com.mendel.transactions.domain.Transaction;
import com.mendel.transactions.domain.TransactionNotFoundException;
import com.mendel.transactions.repository.InMemoryTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

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

        assertThatThrownBy(() -> service.sumWithDescendants(10L)).isInstanceOf(TransactionNotFoundException.class);
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
    void sumsDecimalAmountsExactly() {
        service.save(transaction(10L, "0.1", "cars", null));
        service.save(transaction(11L, "0.2", "cars", 10L));

        assertThat(service.sumWithDescendants(10L)).isEqualByComparingTo("0.3");
    }

    @Test
    void sumsZeroAndNegativeAmounts() {
        service.save(transaction(10L, "0", "cars", null));
        service.save(transaction(11L, "-2500", "refund", 10L));

        assertThat(service.sumWithDescendants(10L)).isEqualByComparingTo("-2500");
        assertThat(service.sumWithDescendants(11L)).isEqualByComparingTo("-2500");
    }

    @Test
    void movingATransactionToAnotherParentUpdatesBothSums() {
        service.save(transaction(10L, "5000", "cars", null));
        service.save(transaction(20L, "1000", "cars", null));
        service.save(transaction(11L, "10000", "shopping", 10L));

        service.save(transaction(11L, "10000", "shopping", 20L));

        assertThat(service.sumWithDescendants(10L)).isEqualByComparingTo("5000");
        assertThat(service.sumWithDescendants(20L)).isEqualByComparingTo("11000");
    }

    @Test
    void rejectedParentLeavesThePreviousTransactionUntouched() {
        service.save(transaction(10L, "5000", "cars", null));
        service.save(transaction(11L, "10000", "shopping", 10L));

        assertThatThrownBy(() -> service.save(transaction(11L, "1", "food", 99L)))
                .isInstanceOf(ParentTransactionNotFoundException.class);

        assertThat(service.sumWithDescendants(10L)).isEqualByComparingTo("15000");
        assertThat(service.sumWithDescendants(11L)).isEqualByComparingTo("10000");
        assertThat(service.findIdsByType("shopping")).containsExactly(11L);
        assertThat(service.findIdsByType("food")).isEmpty();
    }

    @Test
    void rejectedCycleLeavesThePreviousTransactionUntouched() {
        service.save(transaction(10L, "5000", "cars", null));
        service.save(transaction(11L, "10000", "shopping", 10L));

        assertThatThrownBy(() -> service.save(transaction(10L, "1", "food", 11L)))
                .isInstanceOf(CircularTransactionException.class);

        assertThat(service.sumWithDescendants(10L)).isEqualByComparingTo("15000");
        assertThat(service.findIdsByType("cars")).containsExactly(10L);
        assertThat(service.findIdsByType("food")).isEmpty();
    }

    @Test
    @Timeout(10)
    void onlyOneOfTwoOppositeParentsCanBeSaved() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            for (int round = 0; round < 50; round++) {
                service.save(transaction(1L, "10", "a", null));
                service.save(transaction(2L, "10", "a", null));
                CountDownLatch start = new CountDownLatch(1);
                Future<Boolean> first = executor.submit(() -> saveLink(start, 1L, 2L));
                Future<Boolean> second = executor.submit(() -> saveLink(start, 2L, 1L));
                start.countDown();
                boolean savedFirst = first.get(2, TimeUnit.SECONDS);
                boolean savedSecond = second.get(2, TimeUnit.SECONDS);

                assertThat(savedFirst).isNotEqualTo(savedSecond);
                if (savedFirst) {
                    assertThat(service.sumWithDescendants(2L)).isEqualByComparingTo("20");
                    assertThat(service.sumWithDescendants(1L)).isEqualByComparingTo("10");
                } else {
                    assertThat(service.sumWithDescendants(1L)).isEqualByComparingTo("20");
                    assertThat(service.sumWithDescendants(2L)).isEqualByComparingTo("10");
                }
            }
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void changingTypeMovesTheTransaction() {
        service.save(transaction(10L, "5000", "cars", null));

        service.save(transaction(10L, "5000", "shopping", null));

        assertThat(service.findIdsByType("cars")).isEmpty();
        assertThat(service.findIdsByType("shopping")).containsExactly(10L);
    }

    private boolean saveLink(CountDownLatch start, long id, long parentId) throws InterruptedException {
        start.await();
        try {
            service.save(transaction(id, "10", "a", parentId));
            return true;
        } catch (CircularTransactionException exception) {
            return false;
        }
    }

    private static Transaction transaction(long id, String amount, String type, Long parentId) {
        return new Transaction(id, new BigDecimal(amount), type, parentId);
    }
}
