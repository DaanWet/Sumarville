import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LoggingBindingTest {

    @Test
    void slf4jResolvesToARealBindingNotTheNoOpFallback() {
        String factoryClass = LoggerFactory.getILoggerFactory().getClass().getName();
        assertEquals("ch.qos.logback.classic.LoggerContext", factoryClass);
    }
}
