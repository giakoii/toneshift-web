package vn.toneshift.api.common.event;

import java.time.Instant;

/**
 * Something that happened in the domain, published for listeners to react to.
 *
 * <p>Payloads carry ids and primitives only, never JPA entities: a listener may run after the
 * transaction committed and on another thread, where a lazy entity can no longer be loaded.
 */
public interface DomainEvent {

    /**
     * Returns when the event happened.
     *
     * @return the instant the event occurred
     */
    Instant occurredAt();
}
