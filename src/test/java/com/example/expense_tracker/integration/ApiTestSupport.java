package com.example.expense_tracker.integration;

import com.example.expense_tracker.entity.User;
import com.example.expense_tracker.entity.enums.Role;
import com.example.expense_tracker.repository.UserRepository;
import com.jayway.jsonpath.JsonPath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Общие помощники для HTTP-тестов: регистрация и вход пользователей, создание проектов и участников.
 * Все имена уникальны (UUID), поэтому тесты не зависят друг от друга и от порядка запуска.
 */
public abstract class ApiTestSupport extends AbstractIntegrationTest {

    protected static final String PASSWORD = "secret123";

    @Autowired
    protected MockMvc mockMvc;
    @Autowired
    protected UserRepository userRepository;
    @Autowired
    protected PasswordEncoder passwordEncoder;

    protected String uniqueName(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    protected String bearer(String token) {
        return "Bearer " + token;
    }

    protected String tokenFrom(ResultActions result) throws Exception {
        return JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.token");
    }

    protected String register(String username) throws Exception {
        return tokenFrom(mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk()));
    }

    protected String login(String username, String password) throws Exception {
        return tokenFrom(mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk()));
    }

    // Публичного способа создать админа нет (так и задумано), поэтому в тесте сохраняем его напрямую в БД
    protected String createAdmin(String username) throws Exception {
        User admin = new User();
        admin.setUsername(username);
        admin.setPassword(passwordEncoder.encode(PASSWORD));
        admin.setRole(Role.ROLE_ADMIN);
        userRepository.save(admin);
        return login(username, PASSWORD);
    }

    protected String newAdminToken() throws Exception {
        return createAdmin(uniqueName("admin"));
    }

    protected long createProjectId(String adminToken) throws Exception {
        String body = mockMvc.perform(post("/api/v1/projects")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Project " + UUID.randomUUID() + "\",\"description\":\"d\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    protected ResultActions addMember(String adminToken, long projectId, String username) throws Exception {
        return mockMvc.perform(post("/api/v1/projects/" + projectId + "/members")
                .header("Authorization", bearer(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + username + "\"}"));
    }
}
