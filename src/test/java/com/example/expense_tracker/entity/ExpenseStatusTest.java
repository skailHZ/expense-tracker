package com.example.expense_tracker.entity;

import com.example.expense_tracker.entity.enums.ExpenseStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Таблица переходов - ядро бизнес-правил, поэтому проверяем ее полностью, а не выборочно
class ExpenseStatusTest {

    @ParameterizedTest(name = "{0} -> {1} is allowed")
    @CsvSource({
            "PENDING, APPROVED",
            "PENDING, REJECTED",
            "PENDING, CANCELLED",
            "APPROVED, PAID",
            "APPROVED, REJECTED"
    })
    void allowedTransitions(ExpenseStatus from, ExpenseStatus to) {
        assertTrue(from.canTransitionTo(to));
    }

    @ParameterizedTest(name = "{0} -> {1} is forbidden")
    @CsvSource({
            "PENDING, PENDING",
            "PENDING, PAID",
            "APPROVED, PENDING",
            "APPROVED, APPROVED",
            "APPROVED, CANCELLED",
            "REJECTED, PENDING",
            "REJECTED, APPROVED",
            "REJECTED, PAID",
            "PAID, PENDING",
            "PAID, APPROVED",
            "PAID, REJECTED",
            "CANCELLED, PENDING",
            "CANCELLED, APPROVED",
            "CANCELLED, CANCELLED"
    })
    void forbiddenTransitions(ExpenseStatus from, ExpenseStatus to) {
        assertFalse(from.canTransitionTo(to));
    }

    @Test
    void terminalStatuses_HaveNoWayOut() {
        for (ExpenseStatus terminal : new ExpenseStatus[]{ExpenseStatus.PAID, ExpenseStatus.REJECTED, ExpenseStatus.CANCELLED}) {
            assertTrue(Arrays.stream(ExpenseStatus.values()).noneMatch(terminal::canTransitionTo),
                    terminal + " must be terminal");
        }
    }

    @Test
    void totalsCountOnlyLiveExpenses() {
        assertEquals(3, ExpenseStatus.COUNTED_IN_TOTALS.size());
        assertFalse(ExpenseStatus.COUNTED_IN_TOTALS.contains(ExpenseStatus.REJECTED));
        assertFalse(ExpenseStatus.COUNTED_IN_TOTALS.contains(ExpenseStatus.CANCELLED));
    }
}
