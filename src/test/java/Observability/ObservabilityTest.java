package Observability;

import io.sentry.Sentry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ObservabilityTest {

    @AfterEach
    void resetSentry() {
        Sentry.close();
    }

    @Test
    void blankDsnLeavesSentryDisabled() {
        Observability.initSentry(null, "test");
        assertFalse(Sentry.isEnabled());
    }

    @Test
    void blankDsnLeavesSentryDisabled_whitespaceOnly() {
        Observability.initSentry("   ", "test");
        assertFalse(Sentry.isEnabled());
    }

    @Test
    void validDsnEnablesSentry() {
        Observability.initSentry("https://public@example.com/1", "test");
        assertTrue(Sentry.isEnabled());
    }
}
