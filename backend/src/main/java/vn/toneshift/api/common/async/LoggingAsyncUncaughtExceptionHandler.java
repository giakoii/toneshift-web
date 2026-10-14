package vn.toneshift.api.common.async;

import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import vn.toneshift.api.common.logging.AppLog;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * Logs a failure in an asynchronous listener instead of letting it disappear.
 *
 * <p>Deliberately does not rethrow: these listeners run after the originating transaction
 * committed, so failing here must not affect the response the caller already received.
 */
public class LoggingAsyncUncaughtExceptionHandler implements AsyncUncaughtExceptionHandler {

    private static final AppLog log = AppLog.get(LoggingAsyncUncaughtExceptionHandler.class);

    @Override
    public void handleUncaughtException(Throwable throwable, Method method, Object... params) {
        log.error("Listener bất đồng bộ %s thất bại, tham số=%s"
                .formatted(method.getName(), Arrays.toString(params)), throwable);
    }
}
