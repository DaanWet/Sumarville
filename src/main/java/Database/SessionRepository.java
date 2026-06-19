package Database;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Sessions are stored date-only (ISO yyyy-MM-dd), matching the legacy granularity,
 * and returned as {@link LocalDateTime} at start-of-day so existing callers are
 * unchanged.
 */
public class SessionRepository {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE;

    private final Database db;

    public SessionRepository(Database db) {
        this.db = db;
    }

    public void add(String guildId, LocalDateTime date) {
        db.insert("INSERT INTO sessions(guild_id, session_date) VALUES(?,?)",
                guildId, date.toLocalDate().format(ISO));
    }

    public void remove(String guildId, LocalDateTime date) {
        db.update("DELETE FROM sessions WHERE guild_id = ? AND session_date = ?",
                guildId, date.toLocalDate().format(ISO));
    }

    public List<LocalDateTime> find(String guildId, boolean allSessions) {
        List<LocalDateTime> dates = db.query(
                "SELECT session_date FROM sessions WHERE guild_id = ?",
                rs -> LocalDate.parse(rs.getString(1), ISO).atStartOfDay(),
                guildId);
        List<LocalDateTime> result = new ArrayList<>();
        for (LocalDateTime d : dates) {
            if (allSessions || d.isAfter(LocalDateTime.now().minusDays(1))) {
                result.add(d);
            }
        }
        Collections.sort(result);
        return result;
    }
}
