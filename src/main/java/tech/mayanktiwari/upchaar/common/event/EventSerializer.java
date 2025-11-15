package tech.mayanktiwari.upchaar.common.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class EventSerializer {
    private static final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private EventSerializer() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * Serialize event envelope to JSON string
     */
    public static String serialize(EventEnvelope<?> eventEnvelope) {
        try {
            String json = objectMapper.writeValueAsString(eventEnvelope);
            log.debug("Serialized event: {}", json);
            return json;
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize event: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to serialize event", e);
        }
    }

    /**
     * Deserialize JSON string to event envelope
     */
    public static <T> EventEnvelope<T> deserialize(String json, Class<T> eventType) {
        try {
            EventEnvelope<T> eventEnvelope = objectMapper.readValue(json,
                    objectMapper.getTypeFactory().constructParametricType(EventEnvelope.class, eventType));
            log.debug("Deserialized event: {}", eventEnvelope);
            return eventEnvelope;
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize event: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to deserialize event", e);
        }
    }

    public static String toJson(Object object) {
        try {
            String json = objectMapper.writeValueAsString(object);
            log.debug("Serialized object to JSON: {}", json);
            return json;
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize object to JSON: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to serialize object to JSON", e);
        }
    }

    public static <T> T fromJson(String json, Class<T> type) {
        try {
            T object = objectMapper.readValue(json, type);
            log.debug("Deserialized JSON to object: {}", object);
            return object;
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize JSON to object: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to deserialize JSON to object", e);
        }
    }
}
