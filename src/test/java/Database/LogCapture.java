package Database;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.slf4j.LoggerFactory;

import java.util.List;

/** Test utility: records what one class logs while open, so tests can assert on log output. */
final class LogCapture implements AutoCloseable {

    private final Logger logger;
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();

    LogCapture(Class<?> source) {
        logger = (Logger) LoggerFactory.getLogger(source);
        appender.start();
        logger.addAppender(appender);
    }

    List<String> messagesAt(Level level) {
        return appender.list.stream()
                .filter(e -> e.getLevel() == level)
                .map(ILoggingEvent::getFormattedMessage)
                .toList();
    }

    @Override
    public void close() {
        logger.detachAppender(appender);
    }
}
