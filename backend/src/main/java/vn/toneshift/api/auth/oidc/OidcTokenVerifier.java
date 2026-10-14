package vn.toneshift.api.auth.oidc;

import vn.toneshift.api.common.enums.AuthProviderType;

/**
 * Verifies an ID token issued by an external provider.
 */
public interface OidcTokenVerifier {

    /**
     * Verifies an ID token and returns the identity it asserts.
     *
     * @param provider the provider the token is expected to come from
     * @param idToken  the raw ID token presented by the client
     * @return the verified identity
     * @throws vn.toneshift.api.common.exception.UnauthorizedException if the token is malformed, not
     *                                                                signed by the provider, meant
     *                                                                for another application, or expired
     */
    OidcIdentity verify(AuthProviderType provider, String idToken);
}
