package com.example.expense_tracker.integration;

import com.example.expense_tracker.repository.ExpenseRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Жизненный цикл расхода, идемпотентное создание, валюты и аналитика на настоящем PostgreSQL.
 * Параллельные сценарии проверяют именно то, что нельзя проверить моками: гонки на уровне БД.
 */
class ExpenseWorkflowIntegrationTest extends ApiTestSupport {

    @Autowired
    private ExpenseRepository expenseRepository;


    // ---------- жизненный цикл ----------

    @Test
    void expense_GoesThroughFullLifecycle_AndHistoryIsRecorded() throws Exception {
        Project p = newProject();
        long expenseId = createExpenseId(p.memberToken, p.id, "100.00", "RUB", null);

        // Сотрудник не может согласовать собственный расход
        changeStatus(p.memberToken, p.id, expenseId, "APPROVED", null).andExpect(status().isForbidden());

        changeStatus(p.adminToken, p.id, expenseId, "APPROVED", "ok")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
        changeStatus(p.adminToken, p.id, expenseId, "PAID", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"));

        // PAID - конечное состояние
        changeStatus(p.adminToken, p.id, expenseId, "APPROVED", null).andExpect(status().isConflict());
        changeStatus(p.adminToken, p.id, expenseId, "REJECTED", null).andExpect(status().isConflict());

        mockMvc.perform(get(expensesUrl(p.id) + "/" + expenseId + "/history").header("Authorization", bearer(p.memberToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].fromStatus").doesNotExist())
                .andExpect(jsonPath("$[0].toStatus").value("PENDING"))
                .andExpect(jsonPath("$[0].changedBy").value(p.memberName))
                .andExpect(jsonPath("$[1].fromStatus").value("PENDING"))
                .andExpect(jsonPath("$[1].toStatus").value("APPROVED"))
                .andExpect(jsonPath("$[1].comment").value("ok"))
                .andExpect(jsonPath("$[1].changedBy").value(p.adminName))
                .andExpect(jsonPath("$[2].toStatus").value("PAID"));
    }

    @Test
    void rejectedExpense_CannotBePaid() throws Exception {
        Project p = newProject();
        long expenseId = createExpenseId(p.memberToken, p.id, "10.00", "RUB", null);

        changeStatus(p.adminToken, p.id, expenseId, "REJECTED", "no receipt").andExpect(status().isOk());

        changeStatus(p.adminToken, p.id, expenseId, "PAID", null).andExpect(status().isConflict());
    }

    @Test
    void pendingExpense_CannotBeRevertedToPending_Or_PaidDirectly() throws Exception {
        Project p = newProject();
        long expenseId = createExpenseId(p.memberToken, p.id, "10.00", "RUB", null);

        changeStatus(p.adminToken, p.id, expenseId, "PENDING", null).andExpect(status().isConflict());
        changeStatus(p.adminToken, p.id, expenseId, "PAID", null).andExpect(status().isConflict());
    }

    @Test
    void onlyAuthor_CanCancel_AndOnlyWhilePending() throws Exception {
        Project p = newProject();
        long expenseId = createExpenseId(p.memberToken, p.id, "10.00", "RUB", null);
        String otherMemberName = uniqueName("emp");
        String otherMemberToken = register(otherMemberName);
        addMember(p.adminToken, p.id, otherMemberName);

        changeStatus(otherMemberToken, p.id, expenseId, "CANCELLED", null).andExpect(status().isForbidden());
        changeStatus(p.adminToken, p.id, expenseId, "CANCELLED", null).andExpect(status().isForbidden());
        changeStatus(p.memberToken, p.id, expenseId, "CANCELLED", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        // Уже отменен - повторная отмена недопустима
        changeStatus(p.memberToken, p.id, expenseId, "CANCELLED", null).andExpect(status().isConflict());
    }

    @Test
    void authorCannotCancel_AfterApproval() throws Exception {
        Project p = newProject();
        long expenseId = createExpenseId(p.memberToken, p.id, "10.00", "RUB", null);
        changeStatus(p.adminToken, p.id, expenseId, "APPROVED", null).andExpect(status().isOk());

        changeStatus(p.memberToken, p.id, expenseId, "CANCELLED", null).andExpect(status().isConflict());
    }

    @Test
    void expenseOfAnotherProject_IsNotReachableThroughForeignProjectPath() throws Exception {
        Project a = newProject();
        Project b = newProject();
        long expenseOfA = createExpenseId(a.memberToken, a.id, "10.00", "RUB", null);

        // Админ проекта B подставляет id расхода из проекта A в путь своего проекта
        changeStatus(b.adminToken, b.id, expenseOfA, "APPROVED", null).andExpect(status().isNotFound());
        mockMvc.perform(get(expensesUrl(b.id) + "/" + expenseOfA + "/history").header("Authorization", bearer(b.adminToken)))
                .andExpect(status().isNotFound());
        // А через путь проекта A он не имеет доступа вообще
        changeStatus(b.adminToken, a.id, expenseOfA, "APPROVED", null).andExpect(status().isForbidden());
    }

    @Test
    void changeStatus_WithoutStatusOrWithUnknownStatus_Returns400() throws Exception {
        Project p = newProject();
        long expenseId = createExpenseId(p.memberToken, p.id, "10.00", "RUB", null);

        mockMvc.perform(post(expensesUrl(p.id) + "/" + expenseId + "/status")
                        .header("Authorization", bearer(p.adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
        changeStatus(p.adminToken, p.id, expenseId, "TELEPORTED", null).andExpect(status().isBadRequest());
    }

    @Test
    void listExpenses_CanBeFilteredByStatus() throws Exception {
        Project p = newProject();
        long approved = createExpenseId(p.memberToken, p.id, "10.00", "RUB", null);
        createExpenseId(p.memberToken, p.id, "20.00", "RUB", null);
        changeStatus(p.adminToken, p.id, approved, "APPROVED", null).andExpect(status().isOk());

        mockMvc.perform(get(expensesUrl(p.id) + "?status=APPROVED").header("Authorization", bearer(p.adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(approved));
        mockMvc.perform(get(expensesUrl(p.id) + "?status=PENDING").header("Authorization", bearer(p.adminToken)))
                .andExpect(jsonPath("$.totalElements").value(1));
        mockMvc.perform(get(expensesUrl(p.id) + "?status=NOPE").header("Authorization", bearer(p.adminToken)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void concurrentApproveAndReject_ExactlyOneWins() throws Exception {
        Project p = newProject();
        long expenseId = createExpenseId(p.memberToken, p.id, "10.00", "RUB", null);

        List<Callable<Integer>> tasks = List.of(
                () -> changeStatus(p.adminToken, p.id, expenseId, "APPROVED", null).andReturn().getResponse().getStatus(),
                () -> changeStatus(p.adminToken, p.id, expenseId, "REJECTED", null).andReturn().getResponse().getStatus());
        List<Integer> codes = runInParallel(tasks);

        // Победитель получает 200, проигравший - 409 (либо оптимистичная блокировка, либо недопустимый переход)
        assertEquals(1, codes.stream().filter(c -> c == 200).count(), "codes=" + codes);
        assertEquals(1, codes.stream().filter(c -> c == 409).count(), "codes=" + codes);

        // В истории ровно одна запись о переходе: проигравшая транзакция откатилась целиком
        mockMvc.perform(get(expensesUrl(p.id) + "/" + expenseId + "/history").header("Authorization", bearer(p.adminToken)))
                .andExpect(jsonPath("$.length()").value(2));
    }

    // ---------- идемпотентность ----------

    @Test
    void sameKeyAndBody_ReturnsSameExpense_WithoutCreatingDuplicate() throws Exception {
        Project p = newProject();
        String key = UUID.randomUUID().toString();

        MvcResult first = addExpense(p.memberToken, p.id, "75.50", "RUB", key)
                .andExpect(status().isCreated()).andReturn();
        MvcResult second = addExpense(p.memberToken, p.id, "75.50", "RUB", key)
                .andExpect(status().isCreated()).andReturn();

        assertNull(first.getResponse().getHeader("Idempotent-Replayed"));
        assertEquals("true", second.getResponse().getHeader("Idempotent-Replayed"));
        assertEquals(idOf(first), idOf(second));
        mockMvc.perform(get(expensesUrl(p.id)).header("Authorization", bearer(p.adminToken)))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void sameKey_WithEquivalentAmountFormat_IsStillTheSameRequest() throws Exception {
        Project p = newProject();
        String key = UUID.randomUUID().toString();

        MvcResult first = addExpense(p.memberToken, p.id, "10.5", "RUB", key).andExpect(status().isCreated()).andReturn();
        MvcResult second = addExpense(p.memberToken, p.id, "10.50", "RUB", key).andExpect(status().isCreated()).andReturn();

        assertEquals(idOf(first), idOf(second));
    }

    @Test
    void sameKey_WithDifferentBody_Returns422() throws Exception {
        Project p = newProject();
        String key = UUID.randomUUID().toString();
        addExpense(p.memberToken, p.id, "75.50", "RUB", key).andExpect(status().isCreated());

        addExpense(p.memberToken, p.id, "99.99", "RUB", key).andExpect(status().is(422));
        addExpense(p.memberToken, p.id, "75.50", "USD", key).andExpect(status().is(422));
    }

    @Test
    void sameKey_ForDifferentUsers_CreatesSeparateExpenses() throws Exception {
        Project p = newProject();
        String otherName = uniqueName("emp");
        String otherToken = register(otherName);
        addMember(p.adminToken, p.id, otherName);
        String key = UUID.randomUUID().toString();

        long first = idOf(addExpense(p.memberToken, p.id, "5.00", "RUB", key).andExpect(status().isCreated()).andReturn());
        MvcResult secondResult = addExpense(otherToken, p.id, "5.00", "RUB", key).andExpect(status().isCreated()).andReturn();

        // Ключ действует в пределах пользователя: чужой ключ не раскрывает и не блокирует чужой расход
        assertTrue(first != idOf(secondResult));
        assertNull(secondResult.getResponse().getHeader("Idempotent-Replayed"));
    }

    @Test
    void withoutKey_EachRequestCreatesNewExpense() throws Exception {
        Project p = newProject();

        addExpense(p.memberToken, p.id, "5.00", "RUB", null).andExpect(status().isCreated());
        addExpense(p.memberToken, p.id, "5.00", "RUB", null).andExpect(status().isCreated());

        mockMvc.perform(get(expensesUrl(p.id)).header("Authorization", bearer(p.adminToken)))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void invalidKey_Returns400() throws Exception {
        Project p = newProject();

        addExpense(p.memberToken, p.id, "5.00", "RUB", "   ").andExpect(status().isBadRequest());
        addExpense(p.memberToken, p.id, "5.00", "RUB", "k".repeat(256)).andExpect(status().isBadRequest());
    }

    @Test
    void failedValidation_DoesNotConsumeTheKey() throws Exception {
        Project p = newProject();
        String key = UUID.randomUUID().toString();

        addExpense(p.memberToken, p.id, "-1", "RUB", key).andExpect(status().isBadRequest());

        // Ключ не "сгорел": исправленный запрос с тем же ключом проходит как новый
        MvcResult ok = addExpense(p.memberToken, p.id, "1.00", "RUB", key).andExpect(status().isCreated()).andReturn();
        assertNull(ok.getResponse().getHeader("Idempotent-Replayed"));
    }

    @Test
    void forbiddenRequest_DoesNotConsumeTheKey() throws Exception {
        Project p = newProject();
        String strangerName = uniqueName("stranger");
        String strangerToken = register(strangerName);
        String key = UUID.randomUUID().toString();

        addExpense(strangerToken, p.id, "1.00", "RUB", key).andExpect(status().isForbidden());

        addMember(p.adminToken, p.id, strangerName).andExpect(status().isNoContent());
        MvcResult ok = addExpense(strangerToken, p.id, "1.00", "RUB", key).andExpect(status().isCreated()).andReturn();
        assertNull(ok.getResponse().getHeader("Idempotent-Replayed"));
    }

    @Test
    void parallelRequestsWithSameKey_CreateExactlyOneExpense() throws Exception {
        Project p = newProject();
        String key = UUID.randomUUID().toString();
        int parallelism = 8;

        List<Callable<MvcResult>> tasks = new ArrayList<>();
        for (int i = 0; i < parallelism; i++) {
            tasks.add(() -> addExpense(p.memberToken, p.id, "42.00", "RUB", key).andReturn());
        }
        List<MvcResult> results = runInParallel(tasks);

        // Все восемь получили успех и ссылаются на один и тот же расход
        long distinctIds = results.stream().map(this::idOfUnchecked).distinct().count();
        assertTrue(results.stream().allMatch(r -> r.getResponse().getStatus() == 201),
                () -> "statuses=" + results.stream().map(r -> r.getResponse().getStatus()).toList());
        assertEquals(1, distinctIds);
        // Ровно один запрос реально создал расход, остальные - повторы
        assertEquals(parallelism - 1, results.stream()
                .filter(r -> "true".equals(r.getResponse().getHeader("Idempotent-Replayed"))).count());
        // Проверяем напрямую в БД, а не только по ответам API
        assertEquals(1, expenseRepository.findAllByProjectId(p.id, org.springframework.data.domain.Pageable.unpaged()).getTotalElements());
    }

    // ---------- валюта и сумма ----------

    @Test
    void invalidCurrencyOrAmount_Returns400() throws Exception {
        Project p = newProject();

        addExpense(p.memberToken, p.id, "10.00", "XXX", null).andExpect(status().isBadRequest());   // не из списка
        addExpense(p.memberToken, p.id, "10.00", "rub", null).andExpect(status().isBadRequest());   // регистр
        addExpense(p.memberToken, p.id, "10.00", "", null).andExpect(status().isBadRequest());
        addExpense(p.memberToken, p.id, "10.005", "RUB", null).andExpect(status().isBadRequest());  // 3 знака после запятой
        addExpense(p.memberToken, p.id, "123456789.00", "RUB", null).andExpect(status().isBadRequest()); // не влезет в DECIMAL(10,2)
        mockMvc.perform(post(expensesUrl(p.id))
                        .header("Authorization", bearer(p.memberToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":10.00,\"description\":\"no currency\"}"))
                .andExpect(status().isBadRequest());
    }

    // ---------- аналитика ----------

    @Test
    void totals_AreGroupedByCurrency_AndExcludeRejectedAndCancelled() throws Exception {
        Project p = newProject();
        createExpenseId(p.memberToken, p.id, "100.00", "RUB", null);
        createExpenseId(p.memberToken, p.id, "20.00", "USD", null);
        long toReject = createExpenseId(p.memberToken, p.id, "50.00", "RUB", null);
        long toCancel = createExpenseId(p.memberToken, p.id, "7.00", "USD", null);
        changeStatus(p.adminToken, p.id, toReject, "REJECTED", null).andExpect(status().isOk());
        changeStatus(p.memberToken, p.id, toCancel, "CANCELLED", null).andExpect(status().isOk());

        mockMvc.perform(get(expensesUrl(p.id) + "/total").header("Authorization", bearer(p.memberToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].currency").value("RUB"))
                .andExpect(jsonPath("$[0].amount").value(100.00))
                .andExpect(jsonPath("$[1].currency").value("USD"))
                .andExpect(jsonPath("$[1].amount").value(20.00));
    }

    @Test
    void summaryByStatus_CountsEveryStatusAndCurrency() throws Exception {
        Project p = newProject();
        long approved = createExpenseId(p.memberToken, p.id, "30.00", "RUB", null);
        createExpenseId(p.memberToken, p.id, "10.00", "RUB", null);
        createExpenseId(p.memberToken, p.id, "5.00", "RUB", null);
        changeStatus(p.adminToken, p.id, approved, "APPROVED", null).andExpect(status().isOk());

        mockMvc.perform(get(expensesUrl(p.id) + "/analytics/by-status").header("Authorization", bearer(p.memberToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].status").value("PENDING"))
                .andExpect(jsonPath("$[0].count").value(2))
                .andExpect(jsonPath("$[0].total").value(15.00))
                .andExpect(jsonPath("$[1].status").value("APPROVED"))
                .andExpect(jsonPath("$[1].count").value(1))
                .andExpect(jsonPath("$[1].total").value(30.00));
    }

    @Test
    void summaryByEmployee_IsAdminOnly_AndSupportsStatusFilter() throws Exception {
        Project p = newProject();
        String secondName = uniqueName("emp");
        String secondToken = register(secondName);
        addMember(p.adminToken, p.id, secondName);

        long approved = createExpenseId(p.memberToken, p.id, "100.00", "RUB", null);
        createExpenseId(p.memberToken, p.id, "25.00", "RUB", null);
        createExpenseId(secondToken, p.id, "40.00", "USD", null);
        changeStatus(p.adminToken, p.id, approved, "APPROVED", null).andExpect(status().isOk());

        // Участник не видит траты коллег
        mockMvc.perform(get(expensesUrl(p.id) + "/analytics/by-employee").header("Authorization", bearer(p.memberToken)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(expensesUrl(p.id) + "/analytics/by-employee").header("Authorization", bearer(p.adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[?(@.username=='" + p.memberName + "')].count").value(2))
                .andExpect(jsonPath("$[?(@.username=='" + p.memberName + "')].total").value(125.00))
                .andExpect(jsonPath("$[?(@.username=='" + secondName + "')].currency").value("USD"));

        mockMvc.perform(get(expensesUrl(p.id) + "/analytics/by-employee?status=APPROVED").header("Authorization", bearer(p.adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].username").value(p.memberName))
                .andExpect(jsonPath("$[0].total").value(100.00));
    }

    // ---------- помощники ----------

    /** Проект с админом и одним участником-сотрудником. */
    private record Project(long id, String adminName, String adminToken, String memberName, String memberToken) {}

    private Project newProject() throws Exception {
        String adminName = uniqueName("admin");
        String adminToken = createAdmin(adminName);
        String memberName = uniqueName("emp");
        String memberToken = register(memberName);

        MvcResult created = mockMvc.perform(post("/api/v1/projects")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Project " + UUID.randomUUID() + "\",\"description\":\"d\"}"))
                .andExpect(status().isCreated()).andReturn();
        long projectId = ((Number) JsonPath.read(created.getResponse().getContentAsString(), "$.id")).longValue();
        addMember(adminToken, projectId, memberName).andExpect(status().isNoContent());
        return new Project(projectId, adminName, adminToken, memberName, memberToken);
    }

    private String expensesUrl(long projectId) {
        return "/api/v1/projects/" + projectId + "/expenses";
    }

    private ResultActions addExpense(String token, long projectId, String amount, String currency, String idempotencyKey) throws Exception {
        var request = post(expensesUrl(projectId))
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\":" + amount + ",\"currency\":\"" + currency + "\",\"description\":\"test expense\"}");
        if (idempotencyKey != null) {
            request.header("Idempotency-Key", idempotencyKey);
        }
        return mockMvc.perform(request);
    }

    private long createExpenseId(String token, long projectId, String amount, String currency, String key) throws Exception {
        return idOf(addExpense(token, projectId, amount, currency, key).andExpect(status().isCreated()).andReturn());
    }

    private ResultActions changeStatus(String token, long projectId, long expenseId, String status, String comment) throws Exception {
        String body = comment == null
                ? "{\"status\":\"" + status + "\"}"
                : "{\"status\":\"" + status + "\",\"comment\":\"" + comment + "\"}";
        return mockMvc.perform(post(expensesUrl(projectId) + "/" + expenseId + "/status")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private long idOf(MvcResult result) throws Exception {
        return ((Number) JsonPath.read(result.getResponse().getContentAsString(), "$.id")).longValue();
    }

    private long idOfUnchecked(MvcResult result) {
        try {
            return idOf(result);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    // Запускает задачи одновременно (общий старт по защелке), чтобы гарантированно столкнуть их в БД
    private <T> List<T> runInParallel(List<? extends Callable<T>> tasks) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(tasks.size());
        try {
            CountDownLatch ready = new CountDownLatch(tasks.size());
            CountDownLatch go = new CountDownLatch(1);
            List<Future<T>> futures = new ArrayList<>();
            for (Callable<T> task : tasks) {
                futures.add(pool.submit(() -> {
                    ready.countDown();
                    go.await();
                    return task.call();
                }));
            }
            ready.await();
            go.countDown();
            List<T> results = new ArrayList<>();
            for (Future<T> future : futures) {
                results.add(future.get());
            }
            return results;
        } finally {
            pool.shutdownNow();
        }
    }
}
