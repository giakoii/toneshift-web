package vn.toneshift.api.common.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Shared logging facade for the whole backend, routing to the app, sql and error files
 * configured in {@code logback-spring.xml}.
 *
 * <p>Used instead of Lombok's {@code @Slf4j} so that every module logs through one entry point
 * and masking helpers stay consistent. Sensitive values must be passed through {@link LogMasker}.
 */
public final class AppLog {

    /** Logger name wired to the sql file appender. */
    public static final String SQL_LOGGER_NAME = "SQL";

    private static final Logger SQL_LOGGER = LoggerFactory.getLogger(SQL_LOGGER_NAME);

    private final Logger logger;

    // Wraps the delegate logger; instances are created through get(Class).
    private AppLog(Logger logger) {
        this.logger = logger;
    }

    /**
     * Returns a logger named after the given class, so the class name stays visible in the output.
     *
     * @param type the class the log lines belong to
     * @return the logger facade for that class
     */
    public static AppLog get(Class<?> type) {
        return new AppLog(LoggerFactory.getLogger(type));
    }

    /**
     * Logs a statement to the sql file.
     *
     * @param message the message, using SLF4J {@code {}} placeholders
     * @param args    the placeholder arguments
     */
    public static void sql(String message, Object... args) {
        SQL_LOGGER.debug(message, args);
    }

    /**
     * Logs a message at debug level.
     *
     * @param message the message, using SLF4J {@code {}} placeholders
     * @param args    the placeholder arguments
     */
    public void debug(String message, Object... args) {
        logger.debug(message, args);
    }

    /**
     * Logs a message at info level.
     *
     * @param message the message, using SLF4J {@code {}} placeholders
     * @param args    the placeholder arguments
     */
    public void info(String message, Object... args) {
        logger.info(message, args);
    }

    /**
     * Logs a message at warn level.
     *
     * @param message the message, using SLF4J {@code {}} placeholders
     * @param args    the placeholder arguments
     */
    public void warn(String message, Object... args) {
        logger.warn(message, args);
    }

    /**
     * Logs a message at error level, which also reaches the error file.
     *
     * @param message the message, using SLF4J {@code {}} placeholders
     * @param args    the placeholder arguments
     */
    public void error(String message, Object... args) {
        logger.error(message, args);
    }

    /**
     * Logs a message and its cause at error level, which also reaches the error file.
     *
     * @param message   the message
     * @param throwable the cause to log with its stack trace
     */
    public void error(String message, Throwable throwable) {
        logger.error(message, throwable);
    }

    /**
     * Returns whether debug level is enabled, for guarding expensive message construction.
     *
     * @return {@code true} if debug logging is enabled
     */
    public boolean isDebugEnabled() {
        return logger.isDebugEnabled();
    }
}
