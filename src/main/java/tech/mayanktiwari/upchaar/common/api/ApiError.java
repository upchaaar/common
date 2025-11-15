package tech.mayanktiwari.upchaar.common.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiError {
    private String type;
    private String title;
    private int status;
    private String detail;
    private String instance;
    private Instant timestamp;
    private String correlationId;

    public static ApiError of(int status, String title, String details) {
        return ApiError.builder()
                .status(status)
                .title(title)
                .detail(details)
                .timestamp(Instant.now())
                .type("error")
                .build();
    }
}
