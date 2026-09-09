package Observability;

import io.sentry.Sentry;

/** Thin, testable wrapper around Sentry SDK startup so Main stays a plain wiring class. */
public final class Observability {

    private Observability() {
    }

    /**
     * Initializes Sentry. A null/blank dsn leaves the SDK disabled (no-op) rather than throwing —
     * Sentry reporting is entirely optional. IMPORTANT: Sentry.init throws IllegalArgumentException
     * on a null dsn — only an empty string disables the SDK silently — so null must be coerced to "".
     */
    public static void initSentry(String dsn, String environment) {
        Sentry.init(options -> {
            options.setDsn((dsn == null || dsn.isBlank()) ? "" : dsn);
            options.setEnvironment(environment);
        });
    }
}
