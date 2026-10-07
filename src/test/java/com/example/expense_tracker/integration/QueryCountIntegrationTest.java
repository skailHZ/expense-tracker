package com.example.expense_tracker.integration;

import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Защита от проблемы N+1: число SQL-запросов на получение страницы не должно зависеть от числа строк
 * и от числа разных авторов в ней. Если кто-то обратится в маппере к ленивой связи (employee.username,
 * project.name), на каждую строку появится отдельный SELECT, и этот тест упадет.
 */
class QueryCountIntegrationTest extends ApiTestSupport {

    private static final int AUTHORS = 5;
    private static final int ITEMS_PER_AUTHOR = 2;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Test
    void expensePage_QueryCount_DoesNotGrowWithPageSize() throws Exception {
        long small = statementsFor(projectWith(1, 1, Kind.EXPENSE), "/expenses?size=1");
        long large = statementsFor(projectWith(AUTHORS, ITEMS_PER_AUTHOR, Kind.EXPENSE), "/expenses?size=" + AUTHORS * ITEMS_PER_AUTHOR);

        assertEquals(small, large, "expense list: 1 row took " + small + " statements, 10 rows by 5 authors took " + large);
    }

    @Test
    void taskPage_QueryCount_DoesNotGrowWithPageSize() throws Exception {
        long small = statementsFor(projectWith(1, 1, Kind.TASK), "/tasks?size=1");
        long large = statementsFor(projectWith(AUTHORS, ITEMS_PER_AUTHOR, Kind.TASK), "/tasks?size=" + AUTHORS * ITEMS_PER_AUTHOR);

        assertEquals(small, large, "task list: 1 row took " + small + " statements, 10 rows by 5 authors took " + large);
    }

    private enum Kind { EXPENSE, TASK }

    private record Prepared(long projectId, String adminToken) {}

    private Prepared projectWith(int authors, int itemsPerAuthor, Kind kind) throws Exception {
        String adminToken = newAdminToken();
        long projectId = createProjectId(adminToken);
        for (int a = 0; a < authors; a++) {
            String name = uniqueName("emp");
            String token = register(name);
            addMember(adminToken, projectId, name).andExpect(status().isNoContent());
            for (int i = 0; i < itemsPerAuthor; i++) {
                createItem(kind, token, projectId);
            }
        }
        return new Prepared(projectId, adminToken);
    }

    private void createItem(Kind kind, String token, long projectId) throws Exception {
        String url = "/api/v1/projects/" + projectId + (kind == Kind.EXPENSE ? "/expenses" : "/tasks");
        String body = kind == Kind.EXPENSE
                ? "{\"amount\":10.00,\"currency\":\"RUB\",\"description\":\"d\"}"
                : "{\"title\":\"Do it\",\"status\":\"TODO\"}";
        mockMvc.perform(post(url)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());
    }

    private long statementsFor(Prepared project, String pathAndQuery) throws Exception {
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        MockHttpServletRequestBuilder request = get("/api/v1/projects/" + project.projectId() + pathAndQuery)
                .header("Authorization", bearer(project.adminToken()));

        statistics.clear();
        mockMvc.perform(request).andExpect(status().isOk());
        return statistics.getPrepareStatementCount();
    }
}
