package vn.toneshift.api.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import vn.toneshift.api.auth.event.RefreshTokenReuseDetectedEvent;
import vn.toneshift.api.auth.repository.RefreshTokenRepository;
import vn.toneshift.api.common.event.EventPublisher;

import java.time.Instant;
import java.util.UUID;

/**
 * Ends every session of a family whose refresh token leaked.
 */
@Component
@RequiredArgsConstructor
public class RefreshTokenReuseHandler {

    private final RefreshTokenRepository refreshTokens;
    private final EventPublisher events;

    /**
     * Revokes the family and raises the alert, in its own transaction.
     *
     * <p>{@code REQUIRES_NEW}, and a separate bean so the transaction proxy applies, because the
     * caller rejects the request by throwing: the surrounding transaction therefore rolls back. In
     * the caller's transaction this revocation would be rolled back with it, leaving the leaked
     * family usable and never firing the after-commit alert. Committing here keeps the revocation
     * even though the request still fails.
     *
     * @param userId   the owner of the session
     * @param familyId the family to revoke
     * @param now      the revocation instant
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleReuse(UUID userId, UUID familyId, Instant now) {
        refreshTokens.revokeFamily(familyId, now);
        events.publish(new RefreshTokenReuseDetectedEvent(userId, familyId, now));
    }
}
