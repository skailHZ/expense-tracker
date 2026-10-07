package com.example.expense_tracker.repository;

import com.example.expense_tracker.entity.IdempotencyKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, Long> {

    /**
     * Атомарно "занимает" ключ. Возвращает 1, если вставили мы, и 0, если такой ключ уже есть.
     * ON CONFLICT DO NOTHING опирается на UNIQUE (user_id, idempotency_key): если параллельная
     * транзакция уже вставила этот ключ, мы дождемся ее завершения и получим 0 без ошибки.
     */
    @Modifying
    @Query(value = """
            INSERT INTO idempotency_keys (user_id, idempotency_key, request_hash)
            VALUES (:userId, :key, :requestHash)
            ON CONFLICT (user_id, idempotency_key) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(@Param("userId") Long userId,
                       @Param("key") String key,
                       @Param("requestHash") String requestHash);

    Optional<IdempotencyKey> findByUserIdAndKey(Long userId, String key);
}
