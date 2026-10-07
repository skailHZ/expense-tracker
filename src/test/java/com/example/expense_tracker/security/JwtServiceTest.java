package com.example.expense_tracker.security;

import com.example.expense_tracker.entity.User;
import io.jsonwebtoken.security.WeakKeyException;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

    // Ключ только для тестов: 48 байт = 384 бит
    private static final String TEST_KEY = Base64.getEncoder().encodeToString(new byte[48]);

    private JwtService serviceWith(String key, long expirationMs) {
        JwtService service = new JwtService();
        ReflectionTestUtils.setField(service, "secretKey", key);
        ReflectionTestUtils.setField(service, "jwtExpiration", expirationMs);
        return service;
    }

    private User user(String username) {
        User user = new User();
        user.setUsername(username);
        return user;
    }

    @Test
    void startupValidation_RejectsKeyShorterThan256Bits() {
        JwtService service = serviceWith(Base64.getEncoder().encodeToString(new byte[16]), 1000);

        assertThrows(WeakKeyException.class, service::validateSecretKey);
    }

    @Test
    void startupValidation_AcceptsStrongKey() {
        assertDoesNotThrow(() -> serviceWith(TEST_KEY, 1000).validateSecretKey());
    }

    @Test
    void generatedToken_IsValidForItsOwner() {
        JwtService service = serviceWith(TEST_KEY, 60_000);
        User alice = user("alice");

        String token = service.generateToken(alice);

        assertEquals("alice", service.extractUsername(token));
        assertTrue(service.isTokenValid(token, alice));
    }

    @Test
    void token_IsNotValidForAnotherUser() {
        JwtService service = serviceWith(TEST_KEY, 60_000);

        String token = service.generateToken(user("alice"));

        assertFalse(service.isTokenValid(token, user("bob")));
    }

    @Test
    void tokenSignedWithAnotherKey_IsRejected() {
        String otherKey = Base64.getEncoder().encodeToString(new byte[]{
                1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16,
                17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32});
        String forged = serviceWith(otherKey, 60_000).generateToken(user("alice"));

        assertThrows(Exception.class, () -> serviceWith(TEST_KEY, 60_000).extractUsername(forged));
    }
}
