package tech.mayanktiwari.upchaar.common.idempotency;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.util.Optional;

public interface IdempotencyStore {
    /**
     * Save an idempotency record
     */
    void save(IdempotencyRecord record);

    /**
     * Find an existing idempotency record by key
     */
    Optional<IdempotencyRecord> findByKey(String tenantId, String idempotencyKey);

    /**
     * Check if a key exists
     */
    boolean exists(String tenantId, String idempotencyKey);

    /**
     * Delete expired records
     */
    void deleteExpired();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = lombok.AccessLevel.PRIVATE)
    class IdempotencyRecord {
        String id;
        String tenantId;
        String userId;
        String idempotencyKey;
        String requestHash;
        Integer responseStatus;
        String responseBody;
        String resourceLocation;
        Instant createdAt;
        Instant expiresAt;
    }
}
