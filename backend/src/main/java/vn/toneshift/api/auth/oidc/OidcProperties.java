package vn.toneshift.api.auth.oidc;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The external identity providers whose ID tokens this backend accepts, bound from
 * {@code app.oidc}.
 *
 * <p>Each map key is a provider slug matching an {@code AuthProviderType} constant, so adding a
 * provider is a configuration block plus one enum constant.
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.oidc")
public class OidcProperties {

    /** Providers by slug, for example {@code google}. */
    @Valid
    private Map<String, Provider> providers = new LinkedHashMap<>();

    /**
     * Settings of one provider.
     */
    @Getter
    @Setter
    public static class Provider {

        /** Expected {@code iss} claim, compared exactly. */
        @NotBlank
        private String issuer;

        /** This application's client id at the provider; the expected {@code aud} claim. */
        @NotBlank
        private String clientId;

        /** Where to fetch the provider's signing keys. */
        @NotBlank
        private String jwksUri;

        /** Tolerance for clock drift between this server and the provider. */
        private Duration clockSkew = Duration.ofSeconds(60);
    }
}
