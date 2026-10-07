package com.example.expense_tracker.validation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CurrencyValidatorTest {

    private final CurrencyValidator validator = new CurrencyValidator();

    @ParameterizedTest
    @ValueSource(strings = {"RUB", "USD", "EUR", "CNY", "KZT"})
    void supportedCurrencies_AreValid(String code) {
        assertTrue(validator.isValid(code, null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"rub", "XXX", "RU", "RUBL", "", " RUB", "123"})
    void unsupportedValues_AreInvalid(String code) {
        assertFalse(validator.isValid(code, null));
    }

    @Test
    void null_IsLeftToNotBlank() {
        assertTrue(validator.isValid(null, null));
    }
}
