package com.example.expense_tracker.entity.enums;

import java.util.EnumSet;
import java.util.Set;

/**
 * Жизненный цикл расхода (конечный автомат):
 * <pre>
 * PENDING --> APPROVED --> PAID
 *    |            |
 *    |            +-----> REJECTED
 *    +--> REJECTED
 *    +--> CANCELLED (отмена автором)
 * </pre>
 * PAID, REJECTED и CANCELLED - конечные состояния, из них выхода нет.
 */
public enum ExpenseStatus {
    PENDING,
    APPROVED,
    REJECTED,
    PAID,
    CANCELLED;

    // Отклоненные и отмененные расходы не считаются затратами проекта
    public static final Set<ExpenseStatus> COUNTED_IN_TOTALS = EnumSet.of(PENDING, APPROVED, PAID);

    public boolean canTransitionTo(ExpenseStatus target) {
        return switch (this) {
            case PENDING -> target == APPROVED || target == REJECTED || target == CANCELLED;
            case APPROVED -> target == PAID || target == REJECTED;
            case REJECTED, PAID, CANCELLED -> false;
        };
    }
}
