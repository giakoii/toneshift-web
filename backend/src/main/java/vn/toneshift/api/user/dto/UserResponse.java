package vn.toneshift.api.user.dto;

import vn.toneshift.api.common.enums.AuthProviderType;
import vn.toneshift.api.common.enums.Role;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * The signed-in user as returned by the API.
 *
 * @param id          the user id
 * @param name        the display name
 * @param email       the email
 * @param image       the avatar URL, or {@code null}
 * @param role        the role
 * @param hasPassword whether the account can sign in with a password
 * @param providers   the external providers linked to the account
 * @param createdAt   when the account was created
 */
public record UserResponse(
        UUID id,
        String name,
        String email,
        String image,
        Role role,
        boolean hasPassword,
        List<AuthProviderType> providers,
        Instant createdAt) {
}
