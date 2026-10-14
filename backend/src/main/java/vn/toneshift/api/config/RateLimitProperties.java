package vn.toneshift.api.config;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Per-minute allowances for the credential endpoints, bound from {@code app.rate-limit}.
 *
 * <p>Configurable so the limits can be tuned per environment, and so tests can exercise the
 * mechanism without the production numbers getting in the way.
 *
 * @param loginPerMinute          attempts allowed on {@code /api/auth/login} per address
 * @param registerPerMinute       attempts allowed on {@code /api/auth/register} per address
 * @param forgotPasswordPerMinute attempts allowed on {@code /api/auth/forgot-password} per address
 */
@Validated
@ConfigurationProperties(prefix = "app.rate-limit")
public record RateLimitProperties(
        @Min(1) int loginPerMinute,
        @Min(1) int registerPerMinute,
        @Min(1) int forgotPasswordPerMinute) {
}
