package com.example.expense_tracker.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Set;

public class CurrencyValidator implements ConstraintValidator<ValidCurrency, String> {

    // Явный белый список надежнее, чем java.util.Currency: тот пропускает служебные коды вроде XXX
    public static final Set<String> SUPPORTED = Set.of("RUB", "USD", "EUR", "CNY", "KZT", "BYN", "AED", "TRY");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // null проверяет @NotBlank, здесь отвечаем только за формат
        return value == null || SUPPORTED.contains(value);
    }
}
