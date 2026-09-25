import ch.qos.logback.classic.LoggerContext;
import io.sentry.logback.SentryAppender;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class LoggingBindingTest {

    @Test
    void slf4jResolvesToARealBindingNotTheNoOpFallback() {
        String factoryClass = LoggerFactory.getILoggerFactory().getClass().getName();
        assertEquals("ch.qos.logback.classic.LoggerContext", factoryClass);
    }

    /**
     * The production logback.xml has a Sentry appender that initialises Sentry on its own when SENTRY_DSN is
     * in the environment, so tests must use a config without it or their WARNs become production events.
     */
    @Test
    void testLoggingNeverReachesSentry() {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        List<Object> appenders = new ArrayList<>();
        context.getLogger(Logger.ROOT_LOGGER_NAME).iteratorForAppenders().forEachRemaining(appenders::add);

        assertFalse(appenders.isEmpty(), "tests should still log to the console");
        assertFalse(appenders.stream().anyMatch(a -> a instanceof SentryAppender));
    }
}
