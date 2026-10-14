package vn.toneshift.api.auth.listener;

import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import vn.toneshift.api.auth.event.PasswordChangedEvent;
import vn.toneshift.api.auth.service.TokenService;

/**
 * Ends every session of a user whose password changed.
 *
 * <p>A plain listener, so it runs inside the transaction that changed the password: if revoking
 * fails, the password change must fail with it rather than leaving old sessions alive.
 *
 * <p>Every session is revoked, including the caller's. The backend cannot tell which session sent
 * the request, and signing out everywhere is the safer default after a password change.
 */
@Component
@RequiredArgsConstructor
public class RevokeSessionsListener {

    private final TokenService tokenService;

    /**
     * Revokes all refresh tokens of the user.
     *
     * @param event the password change
     */
    @EventListener
    public void onPasswordChanged(PasswordChangedEvent event) {
        tokenService.revokeAll(event.userId());
    }
}
