package vn.toneshift.api.auth.event;

import vn.toneshift.api.common.event.DomainEvent;

import java.time.Instant;
import java.util.UUID;

/**
 * A user's password was set or changed, whether through the profile or a reset.
 *
 * <p>Lets the user module end existing sessions without depending on the token service.
 *
 * @param userId     the user whose password changed
 * @param occurredAt when it changed
 */
public record PasswordChangedEvent(UUID userId, Instant occurredAt) implements DomainEvent {
}
