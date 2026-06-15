package Commands.Calendar;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

import static org.junit.jupiter.api.Assertions.*;

class SessionDateParserTest {

    private final SessionDateParser parser = new SessionDateParser();

    @Test
    void parsesDayMonthYearToStartOfDay() {
        LocalDateTime date = parser.parse("25/12/2026");
        assertEquals(LocalDateTime.of(2026, 12, 25, 0, 0), date);
    }

    @Test
    void formatRoundTrips() {
        assertEquals("25/12/2026", parser.format(parser.parse("25/12/2026")));
    }

    @Test
    void invalidInputThrows() {
        assertThrows(DateTimeParseException.class, () -> parser.parse("2026-12-25"));
    }

    @Test
    void isInFutureUsesTwelveHourGrace() {
        LocalDateTime now = LocalDateTime.of(2026, 6, 15, 18, 0);
        // start of today is 18h before now -> still "future" within the 12h grace? 18h > 12h -> false
        assertFalse(parser.isInFuture(LocalDateTime.of(2026, 6, 15, 0, 0), now.plusHours(13)));
        // tomorrow is clearly in the future
        assertTrue(parser.isInFuture(LocalDateTime.of(2026, 6, 16, 0, 0), now));
    }
}
