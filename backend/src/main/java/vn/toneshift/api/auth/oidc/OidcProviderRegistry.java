package vn.toneshift.api.auth.oidc;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.id.Issuer;
import com.nimbusds.openid.connect.sdk.validators.IDTokenValidator;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import vn.toneshift.api.common.enums.AuthProviderType;
import vn.toneshift.api.common.exception.ApiException;
import vn.toneshift.api.common.exception.ErrorCode;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resolves provider slugs and holds one ID token validator per configured provider.
 *
 * <p>Validators are built on first use and cached, because building one opens a key source.
 */
@Component
@RequiredArgsConstructor
public class OidcProviderRegistry {

    /**
     * Every major OIDC provider signs ID tokens with RS256. A provider that signs with something
     * else would need its algorithm added to {@link OidcProperties.Provider}.
     */
    private static final JWSAlgorithm SIGNING_ALGORITHM = JWSAlgorithm.RS256;

    private final OidcProperties properties;
    private final JwkSourceFactory jwkSourceFactory;
    private final Map<AuthProviderType, IDTokenValidator> validators = new ConcurrentHashMap<>();

    /**
     * Fails startup if a configured slug has no matching {@link AuthProviderType}, so a typo in
     * configuration surfaces on deploy instead of as a failed sign-in.
     */
    @PostConstruct
    void validateConfiguredSlugs() {
        for (String slug : properties.getProviders().keySet()) {
            if (AuthProviderType.fromSlug(slug).isEmpty()) {
                throw new IllegalStateException(
                        "app.oidc.providers." + slug + " không khớp hằng số nào trong AuthProviderType. "
                                + "Thêm hằng số mới hoặc sửa lại key cấu hình.");
            }
        }
    }

    /**
     * Resolves a provider slug from a request path.
     *
     * @param slug the slug taken from the request
     * @return the provider, if it is both known and configured
     * @throws ApiException with {@link ErrorCode#MALFORMED_REQUEST} (HTTP 400) if the slug is
     *                      unknown or not configured, so an unsupported provider is a bad request
     *                      rather than a server error
     */
    public AuthProviderType require(String slug) {
        return AuthProviderType.fromSlug(slug)
                .filter(type -> properties.getProviders().containsKey(type.slug()))
                .orElseThrow(() -> new ApiException(ErrorCode.MALFORMED_REQUEST,
                        "Không hỗ trợ đăng nhập qua nhà cung cấp này"));
    }

    /**
     * Returns the validator for a provider, building it on first use.
     *
     * @param provider the provider to validate tokens for
     * @return the validator configured with that provider's issuer, client id and keys
     */
    public IDTokenValidator validatorFor(AuthProviderType provider) {
        return validators.computeIfAbsent(provider, this::buildValidator);
    }

    // Builds a validator that checks the signature, issuer, audience and expiry in one call.
    private IDTokenValidator buildValidator(AuthProviderType provider) {
        OidcProperties.Provider config = Optional.ofNullable(properties.getProviders().get(provider.slug()))
                .orElseThrow(() -> new IllegalStateException(
                        "Chưa cấu hình app.oidc.providers." + provider.slug()));
        IDTokenValidator validator = new IDTokenValidator(
                new Issuer(config.getIssuer()),
                new ClientID(config.getClientId()),
                new JWSVerificationKeySelector<SecurityContext>(
                        SIGNING_ALGORITHM, jwkSourceFactory.create(config.getJwksUri())),
                null);
        validator.setMaxClockSkew((int) config.getClockSkew().toSeconds());
        return validator;
    }
}
