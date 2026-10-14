package vn.toneshift.api.security;

import org.springframework.http.HttpMethod;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * The single definition of which endpoints are reachable without authentication.
 *
 * <p>Shared by {@code SecurityConfig}, which permits them, and {@link OptionalBearerTokenResolver},
 * which must know the same set to treat a broken token on these routes as a guest instead of a 401.
 * Keeping one list avoids the two drifting apart, which would silently turn a public endpoint into
 * a 401 for signed-in callers.
 */
public final class PublicEndpoints {

    // GET /api/songs/** also covers /api/songs/{id}/comments and /api/songs/{id}/rating-summary,
    // which the API contract lists separately.
    private static final RequestMatcher MATCHER = new OrRequestMatcher(
            new AntPathRequestMatcher("/api/auth/**", HttpMethod.POST.name()),
            new AntPathRequestMatcher("/api/songs/**", HttpMethod.GET.name()),
            new AntPathRequestMatcher("/api/shares/*", HttpMethod.GET.name()),
            new AntPathRequestMatcher("/api/comments/*/replies", HttpMethod.GET.name()),
            new AntPathRequestMatcher("/v3/api-docs/**"),
            new AntPathRequestMatcher("/swagger-ui/**"),
            new AntPathRequestMatcher("/swagger-ui.html"),
            new AntPathRequestMatcher("/actuator/health"));

    // Holder class, not instantiable.
    private PublicEndpoints() {
    }

    /**
     * Returns the matcher covering every endpoint that does not require authentication.
     *
     * @return the public endpoint matcher
     */
    public static RequestMatcher matcher() {
        return MATCHER;
    }
}
