package com.example.expense_tracker.repository;

import com.example.expense_tracker.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Spring Data JPA сам сгенерирует SQL: SELECT * FROM users WHERE username = ?
    Optional<User> findByUsername(String username);

    // Оптимизированный запрос: SELECT EXISTS(SELECT 1 FROM users WHERE username = ?)
    boolean existsByUsername(String username);
}
