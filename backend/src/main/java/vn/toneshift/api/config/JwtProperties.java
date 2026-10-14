package vn.toneshift.api.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * Token settings bound from {@code app.jwt}.
 *
 * @param issuer            the {@code iss} claim this backend puts in, and requires on, access tokens
 * @param accessTtl         how long an access token stays valid
 * @param refreshTtl        how long a refresh token stays valid
 * @param secretOrKey       the base64 encoded HMAC signing key, at least 32 bytes once decoded
 * @param refreshReuseGrace how long after revocation a replayed refresh token is treated as a
 *                          client retry rather than an attack
 */
@Validated
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
        @NotBlank String issuer,
        @NotNull Duration accessTtl,
        @NotNull Duration refreshTtl,
        @NotBlank String secretOrKey,
        @NotNull Duration refreshReuseGrace) {
}
