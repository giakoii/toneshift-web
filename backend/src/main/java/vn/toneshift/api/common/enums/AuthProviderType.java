package vn.toneshift.api.common.enums;

import java.util.Locale;
import java.util.Optional;

/**
 * External identity provider a user can sign in with, stored as a string in {@code auth_providers.provider}.
 *
 * <p>Adding a provider takes two steps and no migration, because the column is a generic
 * {@code varchar(20)}: add a constant here, and add a block under {@code app.oidc.providers}
 * in {@code application.yml} whose key equals this constant's {@link #slug()}.
 */
public enum AuthProviderType {

    GOOGLE;

    /**
     * Returns the lowercase slug used in configuration keys and in the {@code /oauth/{provider}} path.
     *
     * @return the slug of this provider
     */
    public String slug() {
        return name().toLowerCase(Locale.ROOT);
    }

    /**
     * Resolves a provider from its slug, ignoring case and surrounding whitespace.
     *
     * @param slug the slug to resolve, may be {@code null}
     * @return the matching provider, or empty if the slug is blank or unknown
     */
    public static Optional<AuthProviderType> fromSlug(String slug) {
        if (slug == null || slug.isBlank()) {
            return Optional.empty();
        }
        String normalized = slug.trim().toUpperCase(Locale.ROOT);
        for (AuthProviderType type : values()) {
            if (type.name().equals(normalized)) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }
}
