package com.example.expense_tracker.exception;

// Тот же Idempotency-Key прислан с другим телом запроса
public class IdempotencyKeyReuseException extends RuntimeException {

    public IdempotencyKeyReuseException(String message) {
        super(message);
    }
}
