package vn.toneshift.api.auth.oidc;

import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.JWKSourceBuilder;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.stereotype.Component;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

/**
 * Fetches provider signing keys over HTTPS.
 *
 * <p>The builder's defaults already cache keys, refresh them ahead of expiry and rate-limit
 * refetching, so a provider rotating its keys does not cause a burst of requests.
 */
@Component
public class RemoteJwkSourceFactory implements JwkSourceFactory {

    @Override
    public JWKSource<SecurityContext> create(String jwksUri) {
        return JWKSourceBuilder.create(toUrl(jwksUri)).build();
    }

    // Converts the configured endpoint, failing loudly because a bad value is a configuration error.
    private URL toUrl(String jwksUri) {
        try {
            return URI.create(jwksUri).toURL();
        } catch (MalformedURLException | IllegalArgumentException e) {
            throw new IllegalStateException("jwks-uri không phải URL hợp lệ: " + jwksUri, e);
        }
    }
}
