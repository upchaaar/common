package tech.mayanktiwari.upchaar.common.idempotency;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import java.time.Duration;

@Data
@EnableConfigurationProperties(IdempotencyProperties.class)
@ConfigurationProperties(prefix = "upchaar.idempotency")
public class IdempotencyProperties {
    /**
     * Enable or disable idempotency feature
     */
    private boolean enabled = true;

    /**
     * Time in seconds for which the idempotency key is valid
     */
    private Duration ttl = Duration.ofHours(24);

    /**
     * Header name for idempotency key
     */
    private String headerName = "Idempotency-Key";

    /**
     * Store full response body or just metadata
     */
    private boolean storeFullResponse = false;

    /**
     * Maximum response size to store (in bytes, default 5KB)
     */
    private int maxResponseSize = 5120;

}
