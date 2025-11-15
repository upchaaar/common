package tech.mayanktiwari.upchaar.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class EventEnvelop<T> {

    @Builder.Default
    String eventId = UUID.randomUUID().toString();
    String eventType;
    String version;
    String tenantId;
    String correlationId;
    @Builder.Default
    Instant timestamp = Instant.now();
    String userId;
    T payload;

    public static <T> EventEnvelop<T> of(String eventType, String version, String tenantId, T payload) {
        return EventEnvelop.<T>builder()
                .eventType(eventType)
                .version(version)
                .tenantId(tenantId)
                .payload(payload)
                .build();
    }
}
