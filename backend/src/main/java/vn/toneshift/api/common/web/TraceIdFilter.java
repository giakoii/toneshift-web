package vn.toneshift.api.common.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.SecureRandom;

/**
 * Servlet filter that assigns a trace id to each request, exposes it in the logging context and response header.
 */
@Component
@Order(Integer.MIN_VALUE)
public class TraceIdFilter extends OncePerRequestFilter {

    /** Key under which the trace id is stored in the logging context (MDC). */
    public static final String MDC_KEY = "traceId";

    /** Name of the response header carrying the trace id. */
    public static final String HEADER_NAME = "X-Trace-Id";

    private static final String HEX_CHARS = "0123456789abcdef";
    private static final int TRACE_ID_LENGTH = 8;
    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * Generates a trace id, sets it in the MDC and response header, and clears the MDC after the request.
     *
     * @param request     the current HTTP request
     * @param response    the current HTTP response
     * @param filterChain the remaining filter chain
     * @throws ServletException if the downstream filter chain fails
     * @throws IOException      if an I/O error occurs while processing the request
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String traceId = generateTraceId();
        MDC.put(MDC_KEY, traceId);
        response.setHeader(HEADER_NAME, traceId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }

    // Generates a random 8-character lowercase hex trace id.
    private String generateTraceId() {
        StringBuilder sb = new StringBuilder(TRACE_ID_LENGTH);
        for (int i = 0; i < TRACE_ID_LENGTH; i++) {
            sb.append(HEX_CHARS.charAt(RANDOM.nextInt(HEX_CHARS.length())));
        }
        return sb.toString();
    }
}
