package vn.toneshift.api.common.web;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import vn.toneshift.api.common.exception.ErrorCode;
import vn.toneshift.api.common.logging.AppLog;
import vn.toneshift.api.config.RateLimitProperties;

import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Caps how often the credential endpoints can be called from one address, so password and account
 * guessing cannot be run at speed.
 *
 * <p>Counters live in this instance's memory, which is enough while the backend runs as a single
 * instance. Running several would let a caller multiply its allowance by the number of instances,
 * and the counters would then have to move to a shared store such as Redis.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final AppLog log = AppLog.get(RateLimitFilter.class);

    /** Caps memory use: the oldest client is forgotten once this many are tracked. */
    private static final int MAX_TRACKED_CLIENTS = 10_000;

    private final ApiErrorWriter errorWriter;
    private final Map<String, Integer> limitsPerMinute;

    private final Map<String, Bucket> buckets = Collections.synchronizedMap(
            new LinkedHashMap<>(16, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, Bucket> eldest) {
                    return size() > MAX_TRACKED_CLIENTS;
                }
            });

    /**
     * Builds the filter with the configured allowances.
     *
     * @param errorWriter writes the 429 body
     * @param properties  the per-endpoint allowances
     */
    public RateLimitFilter(ApiErrorWriter errorWriter, RateLimitProperties properties) {
        this.errorWriter = errorWriter;
        this.limitsPerMinute = Map.of(
                "/api/auth/login", properties.loginPerMinute(),
                "/api/auth/register", properties.registerPerMinute(),
                "/api/auth/forgot-password", properties.forgotPasswordPerMinute());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Integer limit = limitFor(request);
        if (limit == null) {
            filterChain.doFilter(request, response);
            return;
        }
        ConsumptionProbe probe = bucketFor(request, limit).tryConsumeAndReturnRemaining(1);
        if (probe.isConsumed()) {
            filterChain.doFilter(request, response);
            return;
        }
        long retryAfterSeconds = Math.max(
                TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill()), 1);
        log.warn("Chặn vì quá giới hạn: {} {} từ {}",
                request.getMethod(), request.getRequestURI(), clientIp(request));
        response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfterSeconds));
        errorWriter.write(request, response, ErrorCode.RATE_LIMITED);
    }

    // Returns the per-minute allowance for this request, or null when the endpoint is not limited.
    private Integer limitFor(HttpServletRequest request) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return null;
        }
        return limitsPerMinute.get(request.getRequestURI());
    }

    // One bucket per endpoint and client address.
    private Bucket bucketFor(HttpServletRequest request, int limit) {
        String key = request.getRequestURI() + "|" + clientIp(request);
        return buckets.computeIfAbsent(key, ignored -> Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(limit)
                        .refillGreedy(limit, Duration.ofMinutes(1))
                        .build())
                .build());
    }

    /**
     * Returns the caller's address.
     *
     * <p>Deliberately does not read {@code X-Forwarded-For} directly: that header is set by the
     * client unless a proxy overwrites it, so trusting it would let anyone pick a fresh key per
     * request and ignore the limit. Behind a proxy, set
     * {@code server.forward-headers-strategy=framework} so the servlet container resolves the real
     * address and this method returns it.
     */
    private String clientIp(HttpServletRequest request) {
        return request.getRemoteAddr();
    }
}
