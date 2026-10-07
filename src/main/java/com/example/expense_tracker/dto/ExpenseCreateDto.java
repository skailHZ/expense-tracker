package com.example.expense_tracker.dto;

import com.example.expense_tracker.validation.ValidCurrency;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record ExpenseCreateDto(
        @NotNull(message = "Сумма обязательна")
        @DecimalMin(value = "0.01", message = "Сумма должна быть больше нуля")
        // Колонка DECIMAL(10,2): без этой проверки лишние знаки молча округлялись бы, а большое число давало бы 500
        @Digits(integer = 8, fraction = 2, message = "Сумма: максимум 8 цифр до запятой и 2 после")
        BigDecimal amount,

        @NotBlank(message = "Валюта обязательна")
        @ValidCurrency(message = "Неподдерживаемая валюта")
        String currency,

        @NotBlank(message = "Описание обязательно")
        String description
) {}
