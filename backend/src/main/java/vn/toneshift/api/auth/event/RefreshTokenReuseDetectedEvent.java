package vn.toneshift.api.auth.event;

import vn.toneshift.api.common.event.DomainEvent;

import java.time.Instant;
import java.util.UUID;

/**
 * A refresh token was presented after it had already been rotated and the grace window had passed,
 * which means the token leaked.
 *
 * @param userId     the owner of the session
 * @param familyId   the session family that was revoked in response
 * @param occurredAt when the reuse was detected
 */
public record RefreshTokenReuseDetectedEvent(UUID userId, UUID familyId, Instant occurredAt)
        implements DomainEvent {
}
