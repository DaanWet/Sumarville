package DataHandlers;

import net.dv8tion.jda.api.entities.Guild;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

/**
 * Verwerkt de gegevens van een Guild en zet dit om naar een Java-verwerkbaar formaat.
 */
@SuppressWarnings("unchecked")
public class DataHandler {

    JSONObject jsonObject;
    protected String guild;
    protected Guild g;
    protected JSONObject guildObject;
    final private String PATH = "./Data.json";
    final private Map<String, JSONArray> arrayData = Map.of(
            "food", new JSONArray(),
            "dates", new JSONArray()
    );
    final private Map<String, JSONObject> objectData = Map.of(
            "messages", new JSONObject()
    );

    DataHandler(Guild g) {
        String s = null;
        JSONParser parser = new JSONParser();
        guild = g.getId();
        this.g = g;
        try (FileReader reader = new FileReader(PATH)) {
            jsonObject = ((JSONObject) parser.parse(reader));
            if (!jsonObject.containsKey(g.getId())) {
                JSONArray f = (JSONArray) arrayData.get("food").clone();
                JSONArray food = new JSONArray();
                JSONArray dates = new JSONArray();
                JSONArray characters = new JSONArray();
                JSONObject config = new JSONObject();
                JSONObject messages = new JSONObject();
                JSONObject data = new JSONObject();
                data.put("Food", food);
                data.put("Dates", dates);
                data.put("Characters", characters);
                data.put("Config", config);
                data.put("Messages", messages);
                jsonObject.put(g.getId(), data);
                save();
            }
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
        guildObject = (JSONObject) jsonObject.get(guild);
    }

    void save() {
        try (FileWriter file = new FileWriter(PATH)) {
            file.write(jsonObject.toJSONString());
            file.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
