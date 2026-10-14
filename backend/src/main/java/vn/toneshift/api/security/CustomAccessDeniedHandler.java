package vn.toneshift.api.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import vn.toneshift.api.common.exception.ErrorCode;
import vn.toneshift.api.common.web.ApiErrorWriter;

import java.io.IOException;

/**
 * Returns the API's JSON error body when an authenticated caller lacks the required role.
 */
@Component
@RequiredArgsConstructor
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private final ApiErrorWriter errorWriter;

    /**
     * Writes a 403 error body for a request the caller is not allowed to make.
     *
     * @param request   the current HTTP request
     * @param response  the response to write to
     * @param exception the authorization failure
     * @throws IOException if the response cannot be written
     */
    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException exception) throws IOException {
        errorWriter.write(request, response, ErrorCode.FORBIDDEN);
    }
}
