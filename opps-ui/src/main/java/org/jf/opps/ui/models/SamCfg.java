package org.jf.opps.ui.models;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.util.UUID;

public class SamCfg {

    private UUID uid;
    private String sam_api_key;
    private long sam_api_key_last_change_ms;
    private JsonArray sam_types;
    private JsonArray sam_naics;
    private long last_search_date;

    public long getLast_search_date() {return last_search_date;}
    public void setLast_search_date(long last_search_date) {this.last_search_date = last_search_date;}

    public UUID getUid() {return uid;}
    public void setUid(UUID uid) {this.uid = uid;}

    public String getSam_api_key() {return sam_api_key;}
    public void setSam_api_key(String sam_api_key) {this.sam_api_key = sam_api_key;}

    public long getSam_api_key_last_change_ms() {return sam_api_key_last_change_ms;}
    public void setSam_api_key_last_change_ms(long sam_api_key_last_change_ms) {this.sam_api_key_last_change_ms = sam_api_key_last_change_ms;}

    // json stuff below
    public JsonArray getSam_types() {return sam_types;}
    public void setSam_types(String sam_types) {
        this.sam_types = JsonParser.parseString(sam_types).getAsJsonArray();
    }
    public void setSam_types_json(JsonArray ja){this.sam_types = ja;}

    public JsonArray getSam_naics() {return sam_naics;}
    public void setSam_naics(String sam_naics) {
        this.sam_naics = JsonParser.parseString(sam_naics).getAsJsonArray();
    }
    public void setSam_naics_json(JsonArray ja){this.sam_naics = ja;}

    public boolean hasCode(int code){
        for(JsonElement je : this.sam_naics){
            if(je.getAsJsonObject().get("code").getAsInt() == code){
                return true;
            }
        }
        return false;
    }

    public String getCodeDescription(int code){
        for(JsonElement je : this.sam_naics){
            if(je.getAsJsonObject().get("code").getAsInt() == code){
                if(je.getAsJsonObject().get("desc").isJsonNull()){
                    return null;
                }else {
                    return je.getAsJsonObject().get("desc").getAsString();
                }
            }
        }
        return null;
    }

}
