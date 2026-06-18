package DataHandlers;

import net.dv8tion.jda.api.entities.Guild;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

/**
 * Breidt DataHandler uit met kalenderverwerking
 */
public class CalendarHandler extends DataHandler {

    private DateTimeFormatter storesdf;
    private JSONArray dates;

    public CalendarHandler(Guild g) {
        super(g);
        storesdf = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        dates = (JSONArray) guildObject.get("Dates");
    }


    @SuppressWarnings("unchecked")
    public void addSession(LocalDateTime date) {
        dates.add(date.format(storesdf));
        save();
    }

    public void removeSession(LocalDateTime date) {
        dates.remove(date.format(storesdf));
        save();
    }

    public ArrayList<LocalDateTime> getSessions(boolean allsessions) {
        ArrayList<LocalDateTime> datesList = new ArrayList<>();
        Arrays.stream(dates.toArray()).forEach(date -> {
            try {
                LocalDateTime da = storesdf.parse((String) date, LocalDate::from).atStartOfDay();

                if (allsessions || da.isAfter(LocalDateTime.now().minusDays(1))) {
                    datesList.add(da);
                }
            } catch (DateTimeParseException exc) {
                exc.printStackTrace();
            }
        });

        Collections.sort(datesList);
        return datesList;
    }
}
