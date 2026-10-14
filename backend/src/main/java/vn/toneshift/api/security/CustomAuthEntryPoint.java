package vn.toneshift.api.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import vn.toneshift.api.common.exception.ErrorCode;
import vn.toneshift.api.common.web.ApiErrorWriter;

import java.io.IOException;

/**
 * Returns the API's JSON error body for unauthenticated requests, instead of the default
 * {@code WWW-Authenticate} challenge that would surface as HTML to the frontend.
 */
@Component
@RequiredArgsConstructor
public class CustomAuthEntryPoint implements AuthenticationEntryPoint {

    private final ApiErrorWriter errorWriter;

    /**
     * Writes a 401 error body for a request that is missing or carrying an invalid token.
     *
     * @param request   the current HTTP request
     * @param response  the response to write to
     * @param exception the authentication failure
     * @throws IOException if the response cannot be written
     */
    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException exception) throws IOException {
        errorWriter.write(request, response, ErrorCode.UNAUTHORIZED);
    }
}
