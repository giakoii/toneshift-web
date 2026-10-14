package vn.toneshift.api.auth.service;

import vn.toneshift.api.user.entity.User;

import java.util.UUID;

/**
 * Issues and revokes refresh tokens.
 */
public interface TokenService {

    /**
     * Issues a refresh token for a session family.
     *
     * @param user      the user to issue for
     * @param familyId  the session family; a new value starts a new session
     * @param userAgent the client user agent, may be {@code null}
     * @return the issued token, including its raw value
     */
    IssuedRefreshToken issue(User user, UUID familyId, String userAgent);

    /**
     * Exchanges a refresh token for a new one in the same session family, revoking the old one.
     *
     * @param rawToken  the refresh token presented by the client
     * @param userAgent the client user agent, may be {@code null}
     * @return the session owner and the replacement token
     */
    Rotated rotate(String rawToken, String userAgent);

    /**
     * Revokes every active token of one session family, ending that session.
     *
     * @param familyId the family to revoke
     * @return the number of tokens revoked
     */
    int revokeFamily(UUID familyId);

    /**
     * Revokes every active token of a user, signing them out on all devices.
     *
     * @param userId the user whose sessions to end
     * @return the number of tokens revoked
     */
    int revokeAll(UUID userId);
}
