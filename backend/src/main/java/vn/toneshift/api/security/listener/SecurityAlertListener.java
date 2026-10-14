package vn.toneshift.api.security.listener;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import vn.toneshift.api.auth.event.RefreshTokenReuseDetectedEvent;
import vn.toneshift.api.common.logging.AppLog;

/**
 * Records security-relevant events for operators.
 *
 * <p>Runs after commit and off the request thread, so it never delays or affects the response the
 * caller already gets.
 */
@Component
public class SecurityAlertListener {

    private static final AppLog log = AppLog.get(SecurityAlertListener.class);

    /**
     * Logs a leaked refresh token being replayed.
     *
     * @param event the detected reuse
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRefreshTokenReuse(RefreshTokenReuseDetectedEvent event) {
        log.warn("Phát hiện refresh token bị dùng lại: userId={}, familyId={}, đã thu hồi toàn bộ family",
                event.userId(), event.familyId());
    }
}
