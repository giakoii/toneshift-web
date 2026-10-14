package vn.toneshift.api.auth.oidc;

import vn.toneshift.api.common.enums.AuthProviderType;

/**
 * The identity asserted by a verified ID token.
 *
 * @param provider      the provider that issued the token
 * @param subject       the provider's stable id for the account, its {@code sub} claim
 * @param email         the email claimed by the token
 * @param emailVerified whether the provider states the email is verified
 * @param name          the display name, may be {@code null}
 * @param pictureUrl    the avatar URL, may be {@code null}
 */
public record OidcIdentity(
        AuthProviderType provider,
        String subject,
        String email,
        boolean emailVerified,
        String name,
        String pictureUrl) {
}
