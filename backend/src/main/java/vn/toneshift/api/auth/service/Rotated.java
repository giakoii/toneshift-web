package vn.toneshift.api.auth.service;

import vn.toneshift.api.user.entity.User;

/**
 * The outcome of rotating a refresh token.
 *
 * <p>Carries the entity because this crosses only from {@code TokenService} to {@code AuthService}
 * within the auth module; the rule against exposing entities applies to what leaves the service
 * layer towards controllers.
 *
 * @param user   the owner of the rotated session
 * @param issued the replacement token
 */
public record Rotated(User user, IssuedRefreshToken issued) {
}
