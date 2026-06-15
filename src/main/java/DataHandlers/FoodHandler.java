package DataHandlers;

import net.dv8tion.jda.api.entities.Guild;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Breidt de DataHandler uit voor het verwerken van de voedselfunctionaliteit.
 */
public class FoodHandler extends DataHandler {
    private JSONArray food;

    public FoodHandler(Guild g) {
        super(g);
        food = (JSONArray) ((JSONObject) jsonObject.get(guild)).get("Food");
    }

    @SuppressWarnings("unchecked")
    public void addFood(String name, String emoji) {
        Map<String, String> foodMap = new HashMap<>();
        foodMap.put("Name", name);
        foodMap.put("Emoji", emoji);
        food.add(new JSONObject(foodMap));
        save();
    }

    public void removeFood(int i) {
        food.remove(i);
        save();
    }

    public ArrayList<Map<String, String>> getFood() {
        ArrayList<Map<String, String>> foodList = new ArrayList<>();
        Arrays.stream(food.toArray()).forEach((object -> {
            Map<String, String> f = new HashMap<>();
            JSONObject obj = (JSONObject) object;
            Arrays.stream(obj.keySet().toArray()).forEach(s -> {
                String name = (String) s;
                f.put(name, (String) obj.get(name));
            });

            foodList.add(f);
        }));
        return foodList;
    }

    public int checkFood(String emoji) {
        ArrayList<Map<String, String>> foods = getFood();

        int i = 0;
        while (i < foods.size() && !foods.get(i).get("Emoji").equalsIgnoreCase(emoji)) {
            i++;
        }
        if (i < foods.size()) {
            return i - 1;
        } else {
            return -1;
        }
    }
}
