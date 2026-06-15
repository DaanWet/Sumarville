package DataHandlers;

import net.dv8tion.jda.api.entities.Guild;
import org.json.simple.JSONObject;
@SuppressWarnings("unchecked")
public class ConfigHandler extends DataHandler{

    private JSONObject config;

    public ConfigHandler(Guild g) {
        super(g);
        config = (JSONObject) ((JSONObject) jsonObject.get(guild)).get("Config");
    }

    public String getDMRoleID(){
        return getRole("DM");
    }
    public String getPlayerRoleID(){
        return getRole("Player");
}


    public void setConfig(String id, String key){
        config.put(key, id);
        save();
    }

    public String getRole(String key){
        return (String) config.getOrDefault(key, "0");
    }

    public String getChannel(String key){
        return (String) config.getOrDefault(key, g.getDefaultChannel().getIdLong());
    }
}
