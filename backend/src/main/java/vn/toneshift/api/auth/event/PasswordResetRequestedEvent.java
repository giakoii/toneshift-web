package vn.toneshift.api.auth.event;

import vn.toneshift.api.common.event.DomainEvent;

import java.time.Instant;
import java.util.UUID;

/**
 * A password reset was requested and a token issued.
 *
 * <p>Carries the raw token because only the hash is stored, so this is the one chance to put it in
 * an email. It must never be logged.
 *
 * @param userId     the user who requested the reset
 * @param email      where to send the link
 * @param rawToken   the raw reset token
 * @param expiresAt  when the token stops working
 * @param occurredAt when the reset was requested
 */
public record PasswordResetRequestedEvent(
        UUID userId,
        String email,
        String rawToken,
        Instant expiresAt,
        Instant occurredAt) implements DomainEvent {
}
