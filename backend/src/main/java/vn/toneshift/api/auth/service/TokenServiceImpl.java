package vn.toneshift.api.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.toneshift.api.auth.entity.RefreshToken;
import vn.toneshift.api.auth.repository.RefreshTokenRepository;
import vn.toneshift.api.common.exception.ErrorCode;
import vn.toneshift.api.common.exception.ForbiddenException;
import vn.toneshift.api.common.exception.UnauthorizedException;
import vn.toneshift.api.common.logging.AppLog;
import vn.toneshift.api.common.util.HashUtils;
import vn.toneshift.api.config.JwtProperties;
import vn.toneshift.api.user.entity.User;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Issues and revokes refresh tokens, storing only their hashes.
 */
@Service
@RequiredArgsConstructor
public class TokenServiceImpl implements TokenService {

    private static final AppLog log = AppLog.get(TokenServiceImpl.class);

    /** Width of {@code refresh_tokens.user_agent}. */
    private static final int USER_AGENT_MAX_LENGTH = 300;

    private final RefreshTokenRepository refreshTokens;
    private final RefreshTokenReuseHandler reuseHandler;
    private final JwtProperties properties;

    @Override
    @Transactional
    public IssuedRefreshToken issue(User user, UUID familyId, String userAgent) {
        String rawToken = HashUtils.randomToken();
        Instant expiresAt = Instant.now().plus(properties.refreshTtl());
        RefreshToken saved = refreshTokens.save(new RefreshToken(
                user,
                HashUtils.sha256Hex(rawToken),
                familyId,
                expiresAt,
                truncateUserAgent(userAgent)));
        return new IssuedRefreshToken(saved.getId(), rawToken, familyId, expiresAt);
    }

    @Override
    @Transactional
    public Rotated rotate(String rawToken, String userAgent) {
        RefreshToken stored = refreshTokens.findByTokenHashForUpdate(HashUtils.sha256Hex(rawToken))
                .orElseThrow(() -> new UnauthorizedException(ErrorCode.REFRESH_TOKEN_INVALID));
        Instant now = Instant.now();
        if (stored.isExpiredAt(now)) {
            throw new UnauthorizedException(ErrorCode.REFRESH_TOKEN_INVALID);
        }
        if (stored.isRevoked()) {
            rejectReuse(stored, now);
        }
        User user = stored.getUser();
        if (user.isBlocked()) {
            throw new ForbiddenException(ErrorCode.ACCOUNT_BLOCKED);
        }
        IssuedRefreshToken replacement = issue(user, stored.getFamilyId(), userAgent);
        stored.setRevokedAt(now);
        stored.setReplacedBy(replacement.id());
        return new Rotated(user, replacement);
    }

    @Override
    @Transactional
    public int revokeFamily(UUID familyId) {
        return refreshTokens.revokeFamily(familyId, Instant.now());
    }

    @Override
    @Transactional
    public int revokeAll(UUID userId) {
        return refreshTokens.revokeAllByUser(userId, Instant.now());
    }

    // Always rejects the request, and additionally kills the family when the replay is not
    // explainable as a client retry.
    private void rejectReuse(RefreshToken stored, Instant now) {
        boolean withinGrace = Duration.between(stored.getRevokedAt(), now)
                .compareTo(properties.refreshReuseGrace()) <= 0;
        if (!withinGrace) {
            reuseHandler.handleReuse(stored.getUser().getId(), stored.getFamilyId(), now);
        } else {
            // A client that lost the response to its own rotation retries with the same token.
            // Refusing it is right, but revoking the session would sign the user out over a
            // dropped connection.
            log.debug("Refresh token dùng lại trong thời gian ân hạn, không thu hồi family {}",
                    stored.getFamilyId());
        }
        throw new UnauthorizedException(ErrorCode.REFRESH_TOKEN_INVALID);
    }

    // Clients can send a user agent longer than the column, which would fail the insert.
    private String truncateUserAgent(String userAgent) {
        if (userAgent == null || userAgent.length() <= USER_AGENT_MAX_LENGTH) {
            return userAgent;
        }
        return userAgent.substring(0, USER_AGENT_MAX_LENGTH);
    }
}
