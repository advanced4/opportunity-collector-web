package org.jf.opps.ui.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import org.jf.common.constants.OppsConstants;
import org.jf.common.exceptions.OppsExceptions;
import org.jf.common.utilities.CSV;
import org.jf.common.utilities.Utils;
import org.jf.opps.ui.config.WebConfig;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import org.jf.opps.ui.models.GrantsCfg;
import org.jf.opps.ui.models.SamCfg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ui.Model;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.time.Year;
import java.util.AbstractMap;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class WebUtils {
    private static final Logger log = LoggerFactory.getLogger(WebUtils.class);

    public static void addToCommonModel(String pageTitle, Model model, WebConfig webConfig, Jws<Claims> jwt){
        model.addAttribute("pagetitle", pageTitle);
        model.addAttribute("year", Year.now().getValue());
        model.addAttribute("version", webConfig.getMisc().getVersion());
        model.addAttribute("cname", webConfig.getMisc().getCname());

        if(jwt == null || jwt.getBody() == null || jwt.getBody().getSubject() == null){
            model.addAttribute("username", "not-set");
        }else{
            model.addAttribute("username", jwt.getBody().get(OppsConstants.Jwt.JwtUsername, String.class));
        }

        if(webConfig.getMisc().getDisableloginfordevelopment()){
            model.addAttribute("isadmin", true);
        }else {
            model.addAttribute("isadmin", isAdmin(jwt, webConfig));
        }
    }

    public static boolean isAdmin(Jws<Claims> token, WebConfig webConfig){
        if(webConfig.getMisc().getDisableloginfordevelopment()){
            return true;
        }
        return token != null && token.getBody() != null && token.getBody().get( OppsConstants.Jwt.JwtAdmin, Boolean.class);
    }

    public static UUID getUuid(Jws<Claims> token, WebConfig webConfig){

        // development is a pain w/ ephemeral keys. this is just to get over the hump
        // of getting all major features working. this is just a user in the db, doesn't matter which
        if(webConfig.getMisc().getDisableloginfordevelopment()){
            return UUID.fromString("1d32e52b-2b47-4d11-87f9-8aa517403785");
        }

        if(token != null && token.getBody() != null){
            String uuid = token.getBody().get( OppsConstants.Jwt.JwtUid, String.class);
            if(Utils.isAnyNullOrEmpty(uuid)){
                return null;
            }
            return UUID.fromString(uuid);
        }
        return null;
    }

    // This is just a bonus feature that isn't necessary, but will be nice
    // as long as it lasts. Not clear on how stable this is -- looks like
    // just a hobbyist behind it, but idk
    public static String getNaicsDescription(int code){
        String url = "http://api.naics.us/v0/q?year=2012&code=" + code;
        try {
            AbstractMap.SimpleEntry<Integer, String> response = Utils.httpGetRequest(url);
            if(response.getKey() == 200){
                JsonObject jres = JsonParser.parseString(response.getValue()).getAsJsonObject();
                return jres.get("title").getAsString();
            }else{
                return null;
            }
        } catch (IOException e) {
            log.error("http GET on endpoint {} failed w/ IOException", url);
            return null;
        }
    }

    public static JsonArray getSamOpps(long start_date_ms, long end_date_ms, SamCfg samCfg) throws OppsExceptions.InvalidDateException,
            OppsExceptions.NoOpportunitiesException, OppsExceptions.BadSamCfgException, OppsExceptions.TheirEndpointException, OppsExceptions.ApiException, OppsExceptions.UnauthorizedSamException {

        if(Utils.isAnyNullOrEmpty(samCfg.getSam_api_key())){
            throw new OppsExceptions.BadSamCfgException("Missing API key");
        }

        if(start_date_ms > end_date_ms){
            throw new OppsExceptions.InvalidDateException("start date must be before end date");
        }

        // the client side library gives the timestamp for midnight of today I think so if you set it to $today,
        // that timstamp is greater than the timestamp of right now. so lets just pretend today is tomorrow
        // which is only used right here for sanity checking. this should work just fine because if someone
        // actually does select tomorrow as an end date (which is invalid), it will still fail properly
        long today_s = System.currentTimeMillis() + (86400 * 1000);
        if(end_date_ms > today_s){
            throw new OppsExceptions.InvalidDateException("end date must be before today");
        }

        if(end_date_ms - start_date_ms > OppsConstants.Misc.ms_in_year){
            throw new OppsExceptions.InvalidDateException("search range too large. must be less than 365 days");
        }

        JsonArray ncodes = samCfg.getSam_naics();
        JsonArray ptypes = samCfg.getSam_types();

        if(ncodes.size() < 1 || ptypes.size() < 1){
            throw new OppsExceptions.BadSamCfgException("You must select at least one type & one NAICS code");
        }

        JsonArray opps = new JsonArray();

        String postedFrom = new SimpleDateFormat("MM/dd/yyyy").format(new Date(start_date_ms));
        String postedTo = new SimpleDateFormat("MM/dd/yyyy").format(new Date(end_date_ms));
        // need to format date to MM/dd/YYY


        for(JsonElement ptype: ptypes){
            for(JsonElement ncode: ncodes){
                String url = OppsConstants.Sam.endpoint + "?api_key=" + samCfg.getSam_api_key()
                        + "&limit=1000&postedFrom=" + postedFrom + "&postedTo=" + postedTo
                        + "&ptype=" + ptype.getAsString() + "&ncode=" + ncode.getAsJsonObject().get("code").getAsString();

                try {
                    AbstractMap.SimpleEntry<Integer, String> response = Utils.httpGetRequest(url);
                    if(response.getKey() == 200) {
                        JsonObject jres = JsonParser.parseString(response.getValue()).getAsJsonObject();
                        log.debug(jres.toString());
                        if (jres.keySet().contains("error")) {
                            throw new OppsExceptions.TheirEndpointException("sam.gov returned an error: " + jres.get("error"));
                        }
                        if (jres.keySet().contains("opportunitiesData")) {
                            JsonArray jopps = jres.get("opportunitiesData").getAsJsonArray();
                              for(JsonElement je : jopps) {
                                opps.add(je);
                            }
                        } else {
                            throw new OppsExceptions.ApiException("an error occurred with sam.gov's API");
                        }
                    }else if(response.getKey() == 403){
                        throw new OppsExceptions.UnauthorizedSamException(response.getValue());
                    }else{
                        throw new OppsExceptions.ApiException(response.getValue());
                    }


                } catch (IOException e) {
                    e.printStackTrace();
                    log.error("http GET IOException on {}", url);
                    throw new OppsExceptions.TheirEndpointException("Failure communicating with sam.gov");
                }

                boolean last_loop = false;
                if(ncodes.get(ncodes.size()-1).equals(ncode) && ptypes.get(ptypes.size()-1).equals(ptype)){
                    last_loop = true;
                    log.debug("On the last loop in getting samopps");
                }

                if(!last_loop) {
                    try {
                        //https://gsa.github.io/sam_api/sam/basics.html
                        // this is just to be nice to their API's, since they aren't clear on what the limits are
                        // they say "5 calls per 5 seconds" -- soooo do you mean 1 call/second??? or can you only do 5 calls
                        // at that rate. if the latter, then how long do you have to wait between those "bursts"??
                        log.debug("sleeping for 2 seconds");
                        TimeUnit.SECONDS.sleep(2);
                    } catch (InterruptedException ignored) { }
                }
            }
        }
        return opps;
    }

    public static String getGrantsOppsCsv(GrantsCfg grantsCfg, int pastDays) throws OppsExceptions.InvalidDateException,
            OppsExceptions.BadGrantsCfgException, OppsExceptions.NoOpportunitiesException, OppsExceptions.TheirEndpointException, IOException {
        if(pastDays > 365){
            throw new OppsExceptions.InvalidDateException("Invalid date range. Must be less than 365 days");
        }

        if(pastDays < 1){
            throw new OppsExceptions.InvalidDateException("Invalid date range. Must be at least one day");
        }

        if(grantsCfg.getCats().size() < 1){
            throw new OppsExceptions.BadGrantsCfgException("You must select at least one category");
        }

        if(grantsCfg.getEligibilities().size() < 1){
            throw new OppsExceptions.BadGrantsCfgException("You must select at least one eligibility");
        }

        if(grantsCfg.getInstruments().size() < 1){
            throw new OppsExceptions.BadGrantsCfgException("You must select at least one instrument");
        }

        String cat_param_val = String.join("|", Utils.jsonArrayToArrayList(grantsCfg.getCats()));
        String elig_param_val = String.join("|", Utils.jsonArrayToArrayList(grantsCfg.getEligibilities()));
        String inst_param_val = String.join("|", Utils.jsonArrayToArrayList(grantsCfg.getInstruments()));

        JsonObject GET_payload = new JsonObject();
        GET_payload.addProperty("startRecordNum", 0);
        GET_payload.addProperty("oppStatuses", "forecasted|posted");
        GET_payload.addProperty("sortBy", "openDate|desc");
        GET_payload.addProperty("rows", 9999);
        GET_payload.addProperty("eligibilities", elig_param_val);
        GET_payload.addProperty("fundingCategories", cat_param_val);
        GET_payload.addProperty("fundingInstruments", inst_param_val);
        GET_payload.addProperty("dateRange", pastDays);

        String url = OppsConstants.Grants.endpoint + GET_payload.toString();
        // ya they have a weird api where they escape quotes, but not curly brackets or colons
        // so i can't use a normal URL encoder
        url = url.replace("\"", "%22").replace(" ", "");

        AbstractMap.SimpleEntry<Integer, String> response = Utils.httpGetRequest(url);
        if(response.getKey() == 200){
            return response.getValue();
        }else if(response.getKey() == 404){
            throw new OppsExceptions.NoOpportunitiesException("No results");
        }else{
            throw new OppsExceptions.TheirEndpointException("Grants.gov responded with: " + response.getKey());
        }
    }


}
