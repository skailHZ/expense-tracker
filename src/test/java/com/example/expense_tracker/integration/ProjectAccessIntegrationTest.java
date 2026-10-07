package com.example.expense_tracker.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Сквозные проверки авторизации: HTTP -> JWT-фильтр -> Spring Security -> сервисы -> PostgreSQL.
 * Юнит-тесты с моками не могут доказать, что реальный запрос получит именно 401/403, а не 500.
 */
class ProjectAccessIntegrationTest extends ApiTestSupport {

    // ---------- аутентификация ----------

    @Test
    void register_IgnoresRequestedRole_AndCreatesEmployee() throws Exception {
        String username = uniqueName("emp");

        String token = tokenFrom(mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + username + "\",\"password\":\"" + PASSWORD + "\",\"role\":\"ROLE_ADMIN\"}"))
                .andExpect(status().isOk()));

        // Если бы роль админа прошла, создание проекта было бы разрешено
        createProject(token, "Hack").andExpect(status().isForbidden());
    }

    @Test
    void register_DuplicateUsername_Returns409() throws Exception {
        String username = uniqueName("dup");
        register(username);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(username)))
                .andExpect(status().isConflict());
    }

    @Test
    void login_WrongPassword_Returns401() throws Exception {
        String username = uniqueName("login");
        register(username);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpoint_WithoutToken_Returns401() throws Exception {
        mockMvc.perform(get("/api/v1/projects/1")).andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpoint_WithGarbageToken_Returns401() throws Exception {
        mockMvc.perform(get("/api/v1/projects/1").header("Authorization", "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void bootstrapAdmin_CanLogin_AndCreateProject() throws Exception {
        String adminToken = login("it-admin", "it-admin-pass");

        createProject(adminToken, "Bootstrap project").andExpect(status().isCreated());
    }

    @Test
    void healthEndpoint_IsPublic_ButOtherActuatorEndpointsAreNot() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
        mockMvc.perform(get("/actuator/env")).andExpect(status().isUnauthorized());
    }

    // ---------- доступ к объектам (BOLA) ----------

    @Test
    void nonMember_CannotTouchProject_UntilAdminAddsThem() throws Exception {
        String adminToken = newAdminToken();
        long projectId = createProjectId(adminToken);
        String employee = uniqueName("emp");
        String employeeToken = register(employee);

        // До добавления в проект - 403 на всех эндпоинтах
        mockMvc.perform(get("/api/v1/projects/" + projectId).header("Authorization", bearer(employeeToken)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/projects/" + projectId + "/tasks").header("Authorization", bearer(employeeToken)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/projects/" + projectId + "/expenses").header("Authorization", bearer(employeeToken)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/projects/" + projectId + "/expenses/total").header("Authorization", bearer(employeeToken)))
                .andExpect(status().isForbidden());
        addExpense(employeeToken, projectId, "10.00").andExpect(status().isForbidden());

        addMember(adminToken, projectId, employee).andExpect(status().isNoContent());

        // После добавления - доступ есть
        mockMvc.perform(get("/api/v1/projects/" + projectId).header("Authorization", bearer(employeeToken)))
                .andExpect(status().isOk());
        addExpense(employeeToken, projectId, "10.00").andExpect(status().isCreated());
    }

    @Test
    void member_CannotAddMembers_EvenToTheirOwnProject() throws Exception {
        String adminToken = newAdminToken();
        long projectId = createProjectId(adminToken);
        String employee = uniqueName("emp");
        String employeeToken = register(employee);
        addMember(adminToken, projectId, employee).andExpect(status().isNoContent());

        // Участник не может выдать доступ себе или кому-то еще
        addMember(employeeToken, projectId, employee).andExpect(status().isForbidden());
    }

    @Test
    void anotherAdmin_CannotAccessOrManageForeignProject() throws Exception {
        String ownerToken = newAdminToken();
        long projectId = createProjectId(ownerToken);
        String strangerAdminToken = newAdminToken();

        // Роль ADMIN сама по себе не дает доступа к чужому проекту
        mockMvc.perform(get("/api/v1/projects/" + projectId).header("Authorization", bearer(strangerAdminToken)))
                .andExpect(status().isForbidden());
        addMember(strangerAdminToken, projectId, "it-admin").andExpect(status().isForbidden());
        addExpense(strangerAdminToken, projectId, "1.00").andExpect(status().isForbidden());
    }

    @Test
    void unknownProject_Returns404() throws Exception {
        String adminToken = newAdminToken();

        mockMvc.perform(get("/api/v1/projects/999999").header("Authorization", bearer(adminToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    void addMember_UnknownUser_Returns404_AndRepeatedAddIsIdempotent() throws Exception {
        String adminToken = newAdminToken();
        long projectId = createProjectId(adminToken);
        String employee = uniqueName("emp");
        register(employee);

        addMember(adminToken, projectId, "no-such-user-" + UUID.randomUUID()).andExpect(status().isNotFound());
        addMember(adminToken, projectId, employee).andExpect(status().isNoContent());
        addMember(adminToken, projectId, employee).andExpect(status().isNoContent());
    }

    // ---------- бизнес-логика на реальной БД ----------

    @Test
    void expenses_AreSummedByDatabase_AndPaginated() throws Exception {
        String adminToken = newAdminToken();
        long projectId = createProjectId(adminToken);

        mockMvc.perform(get("/api/v1/projects/" + projectId + "/expenses/total").header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        addExpense(adminToken, projectId, "100.10").andExpect(status().isCreated());
        addExpense(adminToken, projectId, "50.40").andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/projects/" + projectId + "/expenses/total").header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].currency").value("RUB"))
                .andExpect(jsonPath("$[0].amount").value(150.50));
        mockMvc.perform(get("/api/v1/projects/" + projectId + "/expenses?size=1").header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void createExpense_InvalidAmount_Returns400() throws Exception {
        String adminToken = newAdminToken();
        long projectId = createProjectId(adminToken);

        addExpense(adminToken, projectId, "0").andExpect(status().isBadRequest());
        addExpense(adminToken, projectId, "-5").andExpect(status().isBadRequest());
    }

    @Test
    void createTask_UnknownStatus_Returns400() throws Exception {
        String adminToken = newAdminToken();
        long projectId = createProjectId(adminToken);

        mockMvc.perform(post("/api/v1/projects/" + projectId + "/tasks")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Do it\",\"status\":\"NOT_A_STATUS\"}"))
                .andExpect(status().isBadRequest());
    }

    // ---------- помощники ----------

    private String credentials(String username) {
        return "{\"username\":\"" + username + "\",\"password\":\"" + PASSWORD + "\"}";
    }

    private ResultActions createProject(String token, String name) throws Exception {
        return mockMvc.perform(post("/api/v1/projects")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"" + name + "\",\"description\":\"d\"}"));
    }

    private ResultActions addExpense(String token, long projectId, String amount) throws Exception {
        return mockMvc.perform(post("/api/v1/projects/" + projectId + "/expenses")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\":" + amount + ",\"currency\":\"RUB\",\"description\":\"test expense\"}"));
    }
}
