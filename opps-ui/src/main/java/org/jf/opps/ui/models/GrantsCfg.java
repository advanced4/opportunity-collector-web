package org.jf.opps.ui.models;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;

import java.util.Date;
import java.util.UUID;

public class GrantsCfg {

    private UUID uid;
    private JsonArray cats;
    private JsonArray eligibilities;
    private JsonArray instruments;
    private long last_search_date;

    public long getLast_search_date() {return last_search_date;}
    public void setLast_search_date(long last_search_date) {this.last_search_date = last_search_date;}

    public UUID getUid() {return uid;}
    public void setUid(UUID uid) {this.uid = uid;}

    public JsonArray getCats() {return cats;}
    public void setCats(String cats) {
        this.cats = JsonParser.parseString(cats).getAsJsonArray();
    }

    public JsonArray getEligibilities() {return eligibilities;}
    public void setEligibilities(String eligibilities) {
        this.eligibilities = JsonParser.parseString(eligibilities).getAsJsonArray();
    }

    public JsonArray getInstruments() {return instruments;}
    public void setInstruments(String instruments) {
        this.instruments = JsonParser.parseString(instruments).getAsJsonArray();
    }
}
