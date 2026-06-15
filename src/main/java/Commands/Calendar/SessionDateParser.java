package Commands.Calendar;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class SessionDateParser {

    private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /** @throws DateTimeParseException if the input is not dd/MM/yyyy */
    public LocalDateTime parse(String input) {
        return LocalDate.from(fmt.parse(input)).atStartOfDay();
    }

    public String format(LocalDateTime date) {
        return fmt.format(date);
    }

    public boolean isInFuture(LocalDateTime date, LocalDateTime now) {
        return date.isAfter(now.minusHours(12));
    }
}
