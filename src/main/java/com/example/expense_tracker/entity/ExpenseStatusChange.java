package com.example.expense_tracker.entity;

import com.example.expense_tracker.entity.enums.ExpenseStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

// Запись аудита: одна строка на каждое изменение статуса расхода
@Entity
@Table(name = "expense_status_history")
@Getter
@Setter
@NoArgsConstructor
public class ExpenseStatusChange {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expense_id", nullable = false)
    private Expense expense;

    // null у самой первой записи - создания расхода
    @Enumerated(EnumType.STRING)
    @Column(name = "from_status")
    private ExpenseStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false)
    private ExpenseStatus toStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by", nullable = false)
    private User changedBy;

    @Column(length = 500)
    private String comment;

    @CreationTimestamp
    @Column(name = "changed_at", nullable = false, updatable = false)
    private Instant changedAt;
}
