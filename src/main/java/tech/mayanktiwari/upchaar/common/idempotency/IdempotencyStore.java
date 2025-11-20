package tech.mayanktiwari.upchaar.common.idempotency;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Optional;

public interface IdempotencyStore {

    /**
     * Lookup an idempotency record by tenant + key.
     */
    Optional<IdempotencyRecord> findByKey(String tenantId, String idempotencyKey);

    /**
     * Atomically create placeholder if no record exists.
     * Returns true if the placeholder was inserted by this call.
     * Returns false if a record already exists.
     */
    boolean createPlaceholderIfAbsent(IdempotencyRecord record);

    /**
     * Update stored record after the request succeeds.
     */
    void updateResponse(
            String tenantId, String idempotencyKey, int responseStatus, String responseBody, String resourceLocation
    );

    /**
     * Delete placeholder only if the stored requestHash matches.
     * Used when request processing fails so a retry can start fresh.
     */
    void deleteIfPlaceholder(String tenantId, String idempotencyKey, String requestHash);

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
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