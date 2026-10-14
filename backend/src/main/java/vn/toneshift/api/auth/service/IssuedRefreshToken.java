package vn.toneshift.api.auth.service;

import java.time.Instant;
import java.util.UUID;

/**
 * A freshly issued refresh token. The raw value exists only here and in the response: the database
 * stores its hash.
 *
 * @param id        the stored token id, used to link the token it replaces
 * @param rawToken  the raw token handed to the client
 * @param familyId  the session family the token belongs to
 * @param expiresAt when the token stops being valid
 */
public record IssuedRefreshToken(UUID id, String rawToken, UUID familyId, Instant expiresAt) {
}
