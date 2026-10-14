package vn.toneshift.api.common.web;

import org.springframework.core.MethodParameter;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import vn.toneshift.api.common.exception.UnauthorizedException;

import java.util.Optional;
import java.util.UUID;

/**
 * Resolves {@link CurrentUser} parameters from the authentication established by the security chain.
 *
 * <p>Reads the user id from {@code authentication.getName()} rather than casting the principal, so
 * it works both for real access tokens and for the {@code jwt()} post-processor used in tests.
 */
@Component
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    /**
     * Returns whether this resolver handles the given parameter.
     *
     * @param parameter the controller method parameter
     * @return {@code true} for {@code UUID} and {@code Optional<UUID>} parameters annotated with
     *         {@link CurrentUser}
     */
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        if (!parameter.hasParameterAnnotation(CurrentUser.class)) {
            return false;
        }
        Class<?> type = parameter.getParameterType();
        return type.equals(UUID.class) || type.equals(Optional.class);
    }

    /**
     * Resolves the caller's user id.
     *
     * @param parameter     the controller method parameter
     * @param container     the model and view container
     * @param request       the current request
     * @param binderFactory the data binder factory
     * @return the user id, or an empty {@link Optional} for a guest on a public endpoint
     * @throws UnauthorizedException if a required user id is missing
     */
    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer container,
                                  NativeWebRequest request,
                                  WebDataBinderFactory binderFactory) {
        Optional<UUID> userId = currentUserId();
        if (parameter.getParameterType().equals(Optional.class)) {
            return userId;
        }
        return userId.orElseThrow(UnauthorizedException::new);
    }

    // Reads the authenticated user id, or empty when the request is anonymous.
    private Optional<UUID> currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(authentication.getName()));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
