package vn.toneshift.api.auth.oidc;

import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;

/**
 * Supplies the signing keys of a provider.
 *
 * <p>Exists so tests can verify real tokens against a key pair they generated, without reaching
 * the network.
 */
public interface JwkSourceFactory {

    /**
     * Returns a key source for the given JWKS endpoint.
     *
     * @param jwksUri the provider's JWKS endpoint
     * @return the key source to verify that provider's tokens with
     */
    JWKSource<SecurityContext> create(String jwksUri);
}
