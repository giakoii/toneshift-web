package vn.toneshift.api.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

/**
 * Signs and verifies this backend's own access tokens with a symmetric HMAC key.
 *
 * <p>HMAC rather than RSA because the issuer and the only verifier are the same process: nobody
 * else needs to verify these tokens, so an asymmetric keypair would add key distribution and a
 * JWKS endpoint with no benefit. Moving to RSA later means replacing these three beans; tokens
 * issued before the switch simply expire within {@code app.jwt.access-ttl}.
 */
@Configuration
@RequiredArgsConstructor
public class JwtConfig {

    /** HS256 requires a key at least as long as its output. */
    private static final int MIN_SECRET_BYTES = 32;

    private final JwtProperties properties;

    /**
     * Builds the signing key, failing startup rather than at first login if it is unusable.
     *
     * @return the HMAC signing key
     */
    @Bean
    public SecretKey jwtSigningKey() {
        byte[] decoded;
        try {
            decoded = Base64.getDecoder().decode(properties.secretOrKey());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(
                    "app.jwt.secret-or-key (biến môi trường JWT_KEY) phải là base64 hợp lệ. "
                            + "Sinh khoá mới bằng: openssl rand -base64 32", e);
        }
        if (decoded.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "app.jwt.secret-or-key (biến môi trường JWT_KEY) phải dài ít nhất "
                            + MIN_SECRET_BYTES + " byte sau khi giải mã base64, hiện tại là "
                            + decoded.length + " byte. Sinh khoá mới bằng: openssl rand -base64 32");
        }
        return new SecretKeySpec(decoded, "HmacSHA256");
    }

    /**
     * Builds the encoder used to mint access tokens.
     *
     * @param jwtSigningKey the HMAC signing key
     * @return the JWT encoder
     */
    @Bean
    public JwtEncoder jwtEncoder(SecretKey jwtSigningKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(jwtSigningKey));
    }

    /**
     * Builds the decoder used to verify access tokens.
     *
     * <p>Accepting HS256 only is what makes a token forged with {@code alg=none}, or signed with a
     * different key, fail verification.
     *
     * @param jwtSigningKey the HMAC signing key
     * @return the JWT decoder
     */
    @Bean
    public JwtDecoder jwtDecoder(SecretKey jwtSigningKey) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(jwtSigningKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefault(),
                new JwtIssuerValidator(properties.issuer())));
        return decoder;
    }
}
