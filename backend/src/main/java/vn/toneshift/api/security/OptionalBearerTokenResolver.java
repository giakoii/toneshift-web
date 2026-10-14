package vn.toneshift.api.security;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.stereotype.Component;
import vn.toneshift.api.common.logging.AppLog;

/**
 * Resolves the bearer token, but hides an unusable one on public endpoints so the caller is
 * treated as a guest instead of being rejected.
 *
 * <p>Public endpoints return richer data to a signed-in caller, yet must still answer a caller
 * whose token has expired. Returning the token would let {@code BearerTokenAuthenticationFilter}
 * reject the request with 401, and that cannot be undone further down the chain: once the filter
 * throws, the chain is aborted and an {@code AuthenticationEntryPoint} has no way to continue the
 * request as a guest.
 *
 * <p>Deciding this requires actually verifying the token, because an expired token is perfectly
 * well-formed and would pass any cheaper check. The cost is one extra HMAC verification per
 * authenticated request to a public endpoint, which is measured in microseconds.
 */
@Component
@RequiredArgsConstructor
public class OptionalBearerTokenResolver implements BearerTokenResolver {

    private static final AppLog log = AppLog.get(OptionalBearerTokenResolver.class);

    private final DefaultBearerTokenResolver delegate = new DefaultBearerTokenResolver();
    private final JwtDecoder jwtDecoder;

    /**
     * Returns the bearer token to authenticate with, or {@code null} to proceed unauthenticated.
     *
     * @param request the current HTTP request
     * @return the token, or {@code null} if absent, or unusable on a public endpoint
     */
    @Override
    public String resolve(HttpServletRequest request) {
        String token = delegate.resolve(request);
        if (token == null || !PublicEndpoints.matcher().matches(request)) {
            return token;
        }
        try {
            jwtDecoder.decode(token);
            return token;
        } catch (JwtException e) {
            log.debug("Bỏ qua token không dùng được trên endpoint public {}: {}",
                    request.getRequestURI(), e.getMessage());
            return null;
        }
    }
}
