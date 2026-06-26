package Commands.Calendar;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SessionReminderTest {

    private final LocalDateTime session = LocalDateTime.of(2026, 7, 1, 20, 0);

    @Test
    void fullWhenMoreThan6hBefore() {
        assertEquals(SessionReminder.RestartAction.FULL,
                SessionReminder.decide(session.minusHours(6).minusMinutes(1), session));
    }

    @Test
    void sixHourBoundaryIsLunchOnly() { // now == session-6h is the boundary the old 4h code got wrong
        assertEquals(SessionReminder.RestartAction.LUNCH_ONLY,
                SessionReminder.decide(session.minusHours(6), session));
    }

    @Test
    void lunchOnlyWithinReminderWindow() {
        assertEquals(SessionReminder.RestartAction.LUNCH_ONLY,
                SessionReminder.decide(session.minusHours(5), session));
        assertEquals(SessionReminder.RestartAction.LUNCH_ONLY,
                SessionReminder.decide(session.minusMinutes(1), session));
    }

    @Test
    void noneAtOrAfterSession() {
        assertEquals(SessionReminder.RestartAction.NONE, SessionReminder.decide(session, session));
        assertEquals(SessionReminder.RestartAction.NONE, SessionReminder.decide(session.plusHours(1), session));
    }
}
