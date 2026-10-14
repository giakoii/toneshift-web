package vn.toneshift.api.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.toneshift.api.auth.repository.PasswordResetTokenRepository;
import vn.toneshift.api.auth.repository.RefreshTokenRepository;
import vn.toneshift.api.common.logging.AppLog;

import java.time.Duration;
import java.time.Instant;

/**
 * Removes tokens that can no longer be used, so the tables do not grow without bound.
 *
 * <p>Runs on every instance. That is harmless because the deletes are idempotent, but it is why
 * there is no distributed lock yet: one is only needed when a job must run exactly once.
 */
@Component
@RequiredArgsConstructor
public class TokenCleanupJob {

    private static final AppLog log = AppLog.get(TokenCleanupJob.class);

    /** Expired refresh tokens are kept this long so a late replay is still seen as reuse. */
    private static final Duration EXPIRED_REFRESH_RETENTION = Duration.ofDays(7);

    private static final long ONE_HOUR_MS = 3_600_000L;

    private final RefreshTokenRepository refreshTokens;
    private final PasswordResetTokenRepository passwordResetTokens;

    /**
     * Deletes refresh tokens long past expiry and reset tokens that cannot be redeemed.
     */
    @Scheduled(fixedDelay = ONE_HOUR_MS, initialDelay = ONE_HOUR_MS)
    @Transactional
    public void deleteUnusableTokens() {
        Instant now = Instant.now();
        int refreshDeleted = refreshTokens.deleteExpiredBefore(now.minus(EXPIRED_REFRESH_RETENTION));
        int resetDeleted = passwordResetTokens.deleteUnusable(now);
        if (refreshDeleted > 0 || resetDeleted > 0) {
            log.info("Dọn token: xoá {} refresh token hết hạn, {} reset token không còn dùng được",
                    refreshDeleted, resetDeleted);
        }
    }
}
