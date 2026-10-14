package vn.toneshift.api.common.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

class AppLogTest {

    @Test
    void logsUnderTheLoggerNamedAfterTheGivenClass() {
        Logger logger = (Logger) LoggerFactory.getLogger(AppLogTest.class);
        ListAppender<ILoggingEvent> appender = attach(logger);

        try {
            AppLog.get(AppLogTest.class).info("hello {}", "world");

            assertThat(appender.list).singleElement().satisfies(event -> {
                assertThat(event.getLoggerName()).isEqualTo(AppLogTest.class.getName());
                assertThat(event.getFormattedMessage()).isEqualTo("hello world");
                assertThat(event.getLevel()).isEqualTo(Level.INFO);
            });
        } finally {
            logger.detachAppender(appender);
        }
    }

    @Test
    void sqlLogsUnderTheSqlLoggerSoItReachesTheSqlFile() {
        Logger logger = (Logger) LoggerFactory.getLogger(AppLog.SQL_LOGGER_NAME);
        ListAppender<ILoggingEvent> appender = attach(logger);

        try {
            AppLog.sql("select {} from users", "id");

            assertThat(appender.list).singleElement().satisfies(event -> {
                assertThat(event.getLoggerName()).isEqualTo(AppLog.SQL_LOGGER_NAME);
                assertThat(event.getFormattedMessage()).isEqualTo("select id from users");
            });
        } finally {
            logger.detachAppender(appender);
        }
    }

    // Attaches a capturing appender and lowers the level so every event is recorded.
    private ListAppender<ILoggingEvent> attach(Logger logger) {
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.setLevel(Level.DEBUG);
        logger.addAppender(appender);
        return appender;
    }
}
