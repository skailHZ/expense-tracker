package com.example.expense_tracker;

import com.example.expense_tracker.integration.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

// Поднимает весь контекст на настоящем PostgreSQL: проверяет, что миграции Liquibase
// и маппинг Hibernate (ddl-auto: validate) согласованы между собой
class ExpenseTrackerApplicationTests extends AbstractIntegrationTest {

	@Test
	void contextLoads() {
	}

}
