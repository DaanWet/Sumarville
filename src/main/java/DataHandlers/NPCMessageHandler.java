package DataHandlers;

import net.dv8tion.jda.api.entities.Guild;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import java.util.ArrayList;
import java.util.Map;

@SuppressWarnings("unchecked")
public class NPCMessageHandler extends DataHandler{

    private JSONObject messages;


    public NPCMessageHandler(Guild g) {
        super(g);
        if (!guildObject.containsKey("Messages")){
            guildObject.put("Messages", new JSONObject());
            save();
        }
        messages = (JSONObject) guildObject.get("Messages");
        if (!messages.containsKey("Basic")) {
            messages.put("Basic", new JSONArray());
            messages.put("Specific", new JSONArray());
        }
    }

    public ArrayList<Map<String, String>> getBasicMessages(){
        return (ArrayList<Map<String, String>>) messages.get("Basic");
    }
    public ArrayList<Map<String, String>> getSpecificMessages(){
        return (ArrayList<Map<String, String>>) messages.get("Specific");
    }

    public void addMessage(String message, String type, String NPC){
        Map<String, String> map = Map.of(
                "npc", NPC,
                "message", message
        );
        ((JSONArray) messages.get(type)).add(map);
        save();
    }
    public void removeMessage(int i){
        ((JSONArray) messages.get("Basic")).remove(i);
        save();
    }
    public void clearSpecific(){
        ((JSONArray) messages.get("Specific")).clear();
    }

}
