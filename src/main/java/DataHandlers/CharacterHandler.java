package DataHandlers;

import net.dv8tion.jda.api.entities.Guild;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
@SuppressWarnings("unchecked")
public class CharacterHandler extends DataHandler {

    private JSONArray characters;

    public CharacterHandler(Guild g) {
        super(g);
        characters = (JSONArray) ((JSONObject) jsonObject.get(guild)).get("Characters");
    }

    public void addCharacter(Map<String, String> character){
        characters.add(new JSONObject(character));
        save();
    }

    public Map<String, String> getCharacter(String name, String type){
        Map<String, String> characterSheet = null;
        int index = getIndex(name, type);
        if (index != -1){
            characterSheet = (JSONObject) characters.get(index);
        }
        return characterSheet;
    }

    public void removeCharacter(String name, String type){
        int index = getIndex(name, type);
        characters.remove(index);
        save();
    }

    private int getIndex(String name, String type){
        boolean found = false;
        int i = 0;
        while (!found && i < characters.size()){
            JSONObject character = (JSONObject) characters.get(i);
            if (((String)character.get(type)).equalsIgnoreCase(name)){
                found = true;
            } else {
                i ++;
            }
        }
        return found? i : -1;
    }

    public ArrayList<Map<String, String>> getAllCharacters(boolean npc_only){
        ArrayList<Map<String, String>> list = new ArrayList<>();
        characters.forEach(object -> {
            if (!npc_only || ((JSONObject) object).get("userid").equals("")) {
                list.add(((JSONObject) object));
            }
        });
        return list;
    }

    public ArrayList<String> getAllCharacterNames(boolean npc_only){
        ArrayList<String> names = new ArrayList<>();
        getAllCharacters(npc_only).forEach(map -> names.add(map.get("name")));
        return names;
    }

    public void editCharacter(String name, String attribute, String newValue){
        int index = getIndex(name, "name");
        JSONObject character = (JSONObject) characters.get(index);
        character.putIfAbsent(attribute, "");
        character.replace(attribute, newValue);
        save();
    }

    public String getPicture(String name, String type){
        Map<String, String> character = getCharacter(name, type);
        return character == null ? "" : character.getOrDefault("picture", "");
    }

}
