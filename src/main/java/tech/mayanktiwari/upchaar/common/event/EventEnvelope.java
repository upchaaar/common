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
public class EventEnvelope<T> {
    @Builder.Default
    String eventId = UUID.randomUUID()
                         .toString();
    String eventType;
    String version;
    String tenantId;
    String correlationId;
    @Builder.Default
    Instant timestamp = Instant.now();
    String userId;
    T payload;

    public static <T> EventEnvelope<T> of(String eventType, String version, String tenantId, T payload) {
        return EventEnvelope.<T>builder()
                            .eventType(eventType)
                            .version(version)
                            .tenantId(tenantId)
                            .payload(payload)
                            .build();
    }
}
