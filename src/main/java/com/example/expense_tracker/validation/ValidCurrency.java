package com.example.expense_tracker.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// Код валюты должен входить в список поддерживаемых (ISO 4217, заглавные буквы)
@Documented
@Constraint(validatedBy = CurrencyValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidCurrency {

    String message() default "Unsupported currency";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
