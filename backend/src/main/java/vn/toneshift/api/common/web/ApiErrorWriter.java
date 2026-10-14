package vn.toneshift.api.common.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import vn.toneshift.api.common.dto.ErrorResponse;
import vn.toneshift.api.common.exception.ErrorCode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Writes the standard {@link ErrorResponse} body from inside a servlet filter, where a thrown
 * exception would never reach the controller advice.
 */
@Component
@RequiredArgsConstructor
public class ApiErrorWriter {

    private static final String UNKNOWN_TRACE_ID = "unknown";

    private final ObjectMapper objectMapper;

    /**
     * Writes the error body and status for the given code.
     *
     * @param request  the current HTTP request
     * @param response the response to write to
     * @param code     the error code to report
     * @throws IOException if the response cannot be written
     */
    public void write(HttpServletRequest request, HttpServletResponse response, ErrorCode code) throws IOException {
        ErrorResponse body = ErrorResponse.of(
                code, code.defaultMessage(), request.getRequestURI(), traceId());
        response.setStatus(code.status().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getOutputStream(), body);
    }

    // Returns the current request trace id from the logging context, or a placeholder if absent.
    private String traceId() {
        String traceId = MDC.get(TraceIdFilter.MDC_KEY);
        return traceId != null ? traceId : UNKNOWN_TRACE_ID;
    }
}
