package tech.mayanktiwari.upchaar.common.outbox;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import java.time.Duration;

@Data
@EnableConfigurationProperties(OutboxProperties.class)
@ConfigurationProperties(prefix = "upchaar.outbox")
public class OutboxProperties {
    /**
     * Enable or disable outbox pattern
     */
    private boolean enabled = true;

    /**
     * Polling interval for unpublished events
     */
    private Duration pollInterval = Duration.ofSeconds(2);

    /**
     * Batch size for polling
     */
    private int batchSize = 100;

    /**
     * Maximum retry attempts before marking as failed
     */
    private int maxRetryAttempts = 5;

    /**
     * Initial retry delay (exponential backoff)
     */
    private Duration initialRetryDelay = Duration.ofSeconds(1);

    /**
     * Maximum retry delay
     */
    private Duration maxRetryDelay = Duration.ofSeconds(60);

    /**
     * Retention period for successfully published events (default 7 days)
     */
    private Duration retentionPeriod = Duration.ofDays(7);

    /**
     * Enable auto-cleanup of old events
     */
    private boolean autoCleanup = true;

    /**
     * Cleanup schedule (cron expression, default: daily at 2 AM)
     */
    private String cleanupSchedule = "0 0 2 * * ?";
}
