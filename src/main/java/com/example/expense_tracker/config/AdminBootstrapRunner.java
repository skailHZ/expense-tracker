package com.example.expense_tracker.config;

import com.example.expense_tracker.entity.User;
import com.example.expense_tracker.entity.enums.Role;
import com.example.expense_tracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Создает первого администратора при старте, если заданы ADMIN_USERNAME и ADMIN_PASSWORD.
 * Через публичную регистрацию админа получить нельзя, поэтому без этого раннера
 * в чистой базе некому было бы создавать проекты.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminBootstrapRunner implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${application.bootstrap-admin.username:}")
    private String username;

    @Value("${application.bootstrap-admin.password:}")
    private String password;

    @Override
    public void run(ApplicationArguments args) {
        if (username.isBlank() || password.isBlank()) {
            log.info("Bootstrap admin is not configured (ADMIN_USERNAME / ADMIN_PASSWORD), skipping");
            return;
        }
        if (userRepository.existsByUsername(username)) {
            return;
        }

        User admin = new User();
        admin.setUsername(username);
        admin.setPassword(passwordEncoder.encode(password));
        admin.setRole(Role.ROLE_ADMIN);
        userRepository.save(admin);
        log.info("Bootstrap admin '{}' created", username);
    }
}
