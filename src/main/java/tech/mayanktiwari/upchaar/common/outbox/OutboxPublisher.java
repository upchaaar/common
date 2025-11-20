package tech.mayanktiwari.upchaar.common.outbox;

import java.util.List;
import tech.mayanktiwari.upchaar.common.event.EventEnvelope;

public interface OutboxPublisher {

    /** Save an event to the outbox (transactional) */
    void save(EventEnvelope<?> envelope, String aggregateType, String aggregateId);

    /**
     * Publish pending events from outbox to messaging system Returns the number of
     * successfully published events
     */
    int publishPending();

    /** Publish a specific outbox record */
    boolean publish(OutboxEntity outbox);

    /** Mark an outbox record as published */
    void markAsPublished(String outboxId);

    /** Mark an outbox record as failed */
    void markAsFailed(String outboxId, String errorMessage);

    /** Increment retry count and schedule next retry */
    void scheduleRetry(String outboxId, String errorMessage);

    /** Delete old published records (cleanup) */
    int deletePublished(int olderThanDays);

    /** Get pending events count */
    long getPendingCount();

    /** Get failed events for manual intervention */
    List<OutboxEntity> getFailedEvents(int limit);
}
