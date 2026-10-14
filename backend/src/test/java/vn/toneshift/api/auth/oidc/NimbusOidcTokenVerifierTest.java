package vn.toneshift.api.auth.oidc;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import vn.toneshift.api.common.enums.AuthProviderType;
import vn.toneshift.api.common.exception.ApiException;
import vn.toneshift.api.common.exception.ErrorCode;
import vn.toneshift.api.common.exception.UnauthorizedException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Exercises the real {@code IDTokenValidator} against a locally generated key pair, so the
 * signature, issuer, audience and expiry checks are tested without reaching Google.
 */
class NimbusOidcTokenVerifierTest {

    private static final String ISSUER = "https://accounts.google.com";
    private static final String CLIENT_ID = "toneshift-client-id";
    private static final String SUBJECT = "google-sub-123";

    private static RSAKey signingKey;

    private NimbusOidcTokenVerifier verifier;
    private OidcProviderRegistry registry;

    @BeforeAll
    static void generateSigningKey() throws JOSEException {
        signingKey = new RSAKeyGenerator(2048).keyID("test-key").generate();
    }

    @BeforeEach
    void setUp() {
        OidcProperties.Provider provider = new OidcProperties.Provider();
        provider.setIssuer(ISSUER);
        provider.setClientId(CLIENT_ID);
        provider.setJwksUri("https://unused.invalid/jwks");

        OidcProperties properties = new OidcProperties();
        properties.setProviders(Map.of(AuthProviderType.GOOGLE.slug(), provider));

        // The seam: keys come from the generated pair instead of the provider's endpoint.
        JwkSourceFactory jwkSourceFactory =
                jwksUri -> new ImmutableJWKSet<>(new JWKSet(signingKey.toPublicJWK()));
        registry = new OidcProviderRegistry(properties, jwkSourceFactory);
        verifier = new NimbusOidcTokenVerifier(registry);
    }

    @Test
    void acceptsATokenSignedByTheProviderForThisApplication() throws Exception {
        String idToken = sign(claims().claim("email", "khoi@example.com")
                .claim("email_verified", true)
                .claim("name", "Khoi PG")
                .claim("picture", "https://example.com/a.png"));

        OidcIdentity identity = verifier.verify(AuthProviderType.GOOGLE, idToken);

        assertThat(identity.provider()).isEqualTo(AuthProviderType.GOOGLE);
        assertThat(identity.subject()).isEqualTo(SUBJECT);
        assertThat(identity.email()).isEqualTo("khoi@example.com");
        assertThat(identity.emailVerified()).isTrue();
        assertThat(identity.name()).isEqualTo("Khoi PG");
        assertThat(identity.pictureUrl()).isEqualTo("https://example.com/a.png");
    }

    /**
     * The audience check is what stops a token that a different application at the same provider
     * obtained from being replayed here.
     */
    @Test
    void rejectsATokenIssuedForAnotherApplication() throws Exception {
        String idToken = sign(claims().audience("someone-elses-client-id"));

        assertThatThrownBy(() -> verifier.verify(AuthProviderType.GOOGLE, idToken))
                .isInstanceOf(UnauthorizedException.class)
                .extracting(e -> ((UnauthorizedException) e).code())
                .isEqualTo(ErrorCode.INVALID_CREDENTIALS);
    }

    @Test
    void rejectsATokenFromAnotherIssuer() throws Exception {
        String idToken = sign(claims().issuer("https://evil.example.com"));

        assertThatThrownBy(() -> verifier.verify(AuthProviderType.GOOGLE, idToken))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void rejectsAnExpiredToken() throws Exception {
        String idToken = sign(claims()
                .expirationTime(Date.from(Instant.now().minus(10, ChronoUnit.MINUTES))));

        assertThatThrownBy(() -> verifier.verify(AuthProviderType.GOOGLE, idToken))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void rejectsATokenWhoseSignatureWasTampered() throws Exception {
        String idToken = sign(claims());
        String tampered = idToken.substring(0, idToken.length() - 2)
                + (idToken.endsWith("A") ? "B" : "A");

        assertThatThrownBy(() -> verifier.verify(AuthProviderType.GOOGLE, tampered))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void rejectsAValueThatIsNotAToken() {
        assertThatThrownBy(() -> verifier.verify(AuthProviderType.GOOGLE, "definitely-not-a-jwt"))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void reportsAnUnverifiedEmailRatherThanFailingVerification() throws Exception {
        String idToken = sign(claims()
                .claim("email", "khoi@example.com")
                .claim("email_verified", false));

        assertThat(verifier.verify(AuthProviderType.GOOGLE, idToken).emailVerified()).isFalse();
    }

    /**
     * Apple sends {@code email_verified} as a string, so the claim is read defensively.
     */
    @Test
    void readsEmailVerifiedWhenSentAsAString() throws Exception {
        String idToken = sign(claims()
                .claim("email", "khoi@example.com")
                .claim("email_verified", "true"));

        assertThat(verifier.verify(AuthProviderType.GOOGLE, idToken).emailVerified()).isTrue();
    }

    @Test
    void rejectsAnUnknownProviderSlugAsABadRequest() {
        assertThatThrownBy(() -> registry.require("apple"))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).code())
                .isEqualTo(ErrorCode.MALFORMED_REQUEST);
        assertThatThrownBy(() -> registry.require(null)).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> registry.require("")).isInstanceOf(ApiException.class);
    }

    @Test
    void resolvesAConfiguredProviderSlugIgnoringCase() {
        assertThat(registry.require("google")).isEqualTo(AuthProviderType.GOOGLE);
        assertThat(registry.require("GOOGLE")).isEqualTo(AuthProviderType.GOOGLE);
    }

    // The claims a valid Google ID token carries, ready to be overridden per test.
    private JWTClaimsSet.Builder claims() {
        Instant now = Instant.now();
        return new JWTClaimsSet.Builder()
                .issuer(ISSUER)
                .subject(SUBJECT)
                .audience(List.of(CLIENT_ID))
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plus(1, ChronoUnit.HOURS)));
    }

    // Signs the claims with the generated key.
    private String sign(JWTClaimsSet.Builder claims) throws JOSEException {
        SignedJWT jwt = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(signingKey.getKeyID()).build(),
                claims.build());
        jwt.sign(new RSASSASigner(signingKey));
        return jwt.serialize();
    }
}
