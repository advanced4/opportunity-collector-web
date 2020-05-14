package org.jf.opps.ui.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.swagger.annotations.ApiParam;
import org.jf.common.constants.OppsConstants;
import org.jf.common.exceptions.OppsExceptions;
import org.jf.common.models.Response;
import org.jf.common.utilities.Utils;
import org.jf.opps.ui.config.WebConfig;
import org.jf.opps.ui.dao.UserCfgDao;
import io.swagger.annotations.Api;
import org.jf.opps.ui.models.GrantsCfg;
import org.jf.opps.ui.models.SamCfg;
import org.jf.opps.ui.utils.WebUtils;
import org.json.CDL;
import org.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.*;


import static org.jf.common.utilities.SpringUtils.getTokenFromReqCTX;

@RestController
@Api(value = "Get Opportunities")
@RequestMapping("/opps/api/")
public class OppsApi {
    private static final Logger log = LoggerFactory.getLogger(OppsApi.class);
    private WebConfig webConfig;
    private UserCfgDao ucd;

    // spring does this
    OppsApi(UserCfgDao ucd, WebConfig webConfig) {
        this.ucd = ucd;
        this.webConfig = webConfig;
    }

    @RequestMapping(value = "naics/{code}", method = {RequestMethod.GET}, produces= MediaType.APPLICATION_JSON_VALUE)
    public Response.StandardResponse getNaicsDescription(HttpServletResponse httpResponse, @PathVariable("code") Integer code) {
        Response.StandardResponse res = new Response.StandardResponse();

        if(Utils.isAnyNullOrEmpty(code)){
            res.setMsg(OppsConstants.Strings.ERR_MISSING_PARAMS);
            httpResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return res;
        }

        // magic numbers:
        // 11 is the smallest & shortest valid NACIS code,
        // the largest NAICS code is 6 digits -- i.e. must not be greater than 999999
        if(code < 11 || code > 999999){
            res.setMsg(OppsConstants.Strings.ERR_INVALID_PARAMS);
            httpResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return res;
        }

        res.setMsg(WebUtils.getNaicsDescription(code));
        res.setStatus(Response.Status.ok);

        return res;
    }

    // yeah kind of weird to require timestamp's in milliseconds, when we only care about day level date resolution
    // but for parsing reasons this is an easier format to work with. everyone understands timestamps in ms
    // and most frameworks generate them with that level of precision, so leave it be (as opposed to seconds or minutes)
    @RequestMapping(value = "sam", method = {RequestMethod.GET})
    public ResponseEntity<?> getSamOpps(@ApiParam(type="query", value="start date timestamp in ms", required=true)Long start,
                                        @ApiParam(type="query", value="end date timestamp in ms")Long end,
                                        @ApiParam(type="query", value="sam.gov solicitation types as a JSON array", required=true)String types,
                                        @ApiParam(type="query", value="a list of valid NAICS codes as a JSON array", required=true)String codes) {

        Response.StandardResponse res = new Response.StandardResponse();

        log.debug("start: {}, end: {}, types: {}, codes: {}", start, end, types, codes);

        if(Utils.isAnyNullOrEmpty(start, types, codes)){
            res.setMsg(OppsConstants.Strings.ERR_MISSING_PARAMS);
            return ResponseEntity.status(HttpServletResponse.SC_BAD_REQUEST).contentType(MediaType.APPLICATION_JSON).body(res);
        }

        // default to today if user ommitted it
        if(Utils.isAnyNullOrEmpty(end)){
            end = System.currentTimeMillis();
        }

        JsonArray ja_types = JsonParser.parseString(types).getAsJsonArray();
        JsonArray ja_codes = JsonParser.parseString(codes).getAsJsonArray();

        if(Utils.isAnyNullOrEmpty(ja_codes, ja_types)){
            res.setMsg(OppsConstants.Strings.ERR_MISSING_PARAMS);
            return ResponseEntity.status(HttpServletResponse.SC_BAD_REQUEST).contentType(MediaType.APPLICATION_JSON).body(res);
        }

        JsonArray newNaics = new JsonArray();
        UUID uid = WebUtils.getUuid(getTokenFromReqCTX(), webConfig);

        SamCfg sc;
        sc = ucd.getSamCfgWithKey(uid);

        for(JsonElement je : ja_codes){
            JsonObject newEntry = new JsonObject();
            int temp_code = je.getAsInt();
            newEntry.addProperty("code", temp_code);
            // we do this to try and avoid hitting the NAICS api too much.
            // hopefully we just do a 1 time burst when a user adds all their codes,
            // then just the occasional addition or deletion
            if(sc.hasCode(temp_code)){
                newEntry.addProperty("desc", sc.getCodeDescription(temp_code));
            }else {
                newEntry.addProperty("desc", WebUtils.getNaicsDescription(je.getAsInt()));
            }
            newNaics.add(newEntry);
        }

        // by doing this, we don't have to query the DB after setting the new config
        sc.setSam_naics_json(newNaics);
        sc.setSam_types_json(ja_types);

        try {
            if(!ucd.setSamCfgOnQuery(uid, ja_types, newNaics)){
                res.setMsg("Failed updating SAM cfg");
                return ResponseEntity.status(HttpServletResponse.SC_INTERNAL_SERVER_ERROR).contentType(MediaType.APPLICATION_JSON).body(res);
            }
        } catch (OppsExceptions.BadSamCfgException e) {
            res.setMsg(e.getMessage());
            return ResponseEntity.status(HttpServletResponse.SC_BAD_REQUEST).contentType(MediaType.APPLICATION_JSON).body(res);
        }

        JsonArray opps;
        try {
            opps = WebUtils.getSamOpps(start, end, sc);
        } catch (OppsExceptions.InvalidDateException | OppsExceptions.TheirEndpointException | OppsExceptions.BadSamCfgException e) {
            res.setMsg(e.getMessage());
            return ResponseEntity.status(HttpServletResponse.SC_BAD_REQUEST).contentType(MediaType.APPLICATION_JSON).body(res);
        } catch (OppsExceptions.NoOpportunitiesException e) {
            res.setMsg("No results");
            res.setStatus(Response.Status.ok);
            return ResponseEntity.status(HttpServletResponse.SC_NO_CONTENT).body(res);
        } catch (OppsExceptions.ApiException e) {
            res.setMsg(e.getMessage());
            return ResponseEntity.status(HttpServletResponse.SC_BAD_GATEWAY).contentType(MediaType.APPLICATION_JSON).body(res);
        } catch (OppsExceptions.UnauthorizedSamException e) {
            res.setMsg("Unauthorized for sam.gov");
            return ResponseEntity.status(HttpServletResponse.SC_UNAUTHORIZED).contentType(MediaType.APPLICATION_JSON).body(res);
        }

        if(opps.size() == 0){
            res.setMsg("No results");
            res.setStatus(Response.Status.ok);
            return ResponseEntity.status(HttpServletResponse.SC_NO_CONTENT).body(res);
        }

        // ugh this is converting between GSON & org.json libraries. I use gson, but it doesn't support converting to CSV
        // but org.json does. this is not an efficient or pretty solution. It is also very confusing since
        // JsonArray (gson) vs JSONArray (org.json)
        String csv_rows;

        try {
            JSONArray ja = new JSONArray(opps.toString());
            csv_rows = CDL.toString(ja);
        } catch(Exception e) {
            e.printStackTrace();
            res.setMsg(e.getMessage());
            return ResponseEntity.status(HttpServletResponse.SC_INTERNAL_SERVER_ERROR).contentType(MediaType.APPLICATION_JSON).body(res);
        }

        String past_date = new SimpleDateFormat("yyyy/MM/dd").format(new Date(start));
        String todays_date = new SimpleDateFormat("yyyy/MM/dd").format(new Date(end));
        String filename = past_date + "_" + todays_date + "_sam.csv";

        HttpHeaders header = new HttpHeaders();
        header.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename="+filename);
        header.add("Cache-Control", "no-cache, no-store, must-revalidate");
        header.add("Pragma", "no-cache");
        header.add("Expires", "0");

        byte[] csvAsBytes = csv_rows.getBytes();
        Resource resource = new ByteArrayResource(csvAsBytes);

        return ResponseEntity.ok()
                .headers(header)
                .contentLength(csvAsBytes.length)
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(resource);
    }

    @RequestMapping(value = "grants", method = {RequestMethod.GET})
    public ResponseEntity<?> getGrantsOpps(
                                           @ApiParam(type="query", value="number of past days to search, integer", required=true)Integer pd,
                                           @ApiParam(type="query", value="grants.gov category as json_array", required=true)String cat,
                                           @ApiParam(type="query", value="grants.gov instrument as json_array", required=true)String inst,
                                           @ApiParam(type="query", value="grants.gov eligiblities as json_array", required=true)String elig ) {
        Response.StandardDataResponse res = new Response.StandardDataResponse();

        log.debug("days: {}, cat: {}, inst: {}, elig: {}", pd, cat, inst, elig);

        if(Utils.isAnyNullOrEmpty(pd, cat, inst, elig)){
            res.setMsg(OppsConstants.Strings.ERR_MISSING_PARAMS);
            return ResponseEntity.status(HttpServletResponse.SC_BAD_REQUEST).contentType(MediaType.APPLICATION_JSON).body(res);
        }

        JsonArray ja_cats = JsonParser.parseString(cat).getAsJsonArray();
        JsonArray ja_elig = JsonParser.parseString(elig).getAsJsonArray();
        JsonArray ja_inst = JsonParser.parseString(inst).getAsJsonArray();

        UUID uid = WebUtils.getUuid(getTokenFromReqCTX(), webConfig);

        try {
            if(!ucd.setGrantsCfgOnQuery(uid, ja_inst, ja_elig, ja_cats)){
                res.setMsg("Failed updating SAM cfg");
                return ResponseEntity.status(HttpServletResponse.SC_INTERNAL_SERVER_ERROR).contentType(MediaType.APPLICATION_JSON).body(res);
            }
        } catch (OppsExceptions.BadGrantsCfgException e) {
            res.setMsg(e.getMessage());
            return ResponseEntity.status(HttpServletResponse.SC_BAD_REQUEST).contentType(MediaType.APPLICATION_JSON).body(res);
        }

        GrantsCfg gc = ucd.getGrantsCfg(uid);
        String csv;
        try {
            csv = WebUtils.getGrantsOppsCsv(gc, pd);
        } catch (OppsExceptions.InvalidDateException | OppsExceptions.BadGrantsCfgException | OppsExceptions.TheirEndpointException e) {
            res.setMsg(e.getMessage());
            return ResponseEntity.status(HttpServletResponse.SC_BAD_REQUEST).contentType(MediaType.APPLICATION_JSON).body(res);
        } catch (IOException e) {
            e.printStackTrace();
            res.setMsg("There was an error communicating with grants.gov");
            return ResponseEntity.status(HttpServletResponse.SC_BAD_GATEWAY).contentType(MediaType.APPLICATION_JSON).body(res);
        } catch (OppsExceptions.NoOpportunitiesException e) {
            res.setMsg("No results");
            res.setStatus(Response.Status.ok);
            return ResponseEntity.ok(res);
        }

        Instant now = Instant.now(); //current date
        Instant before = now.minus(Duration.ofDays(pd)); // minus N days
        Date dateBefore = Date.from(before); // make it a Date() object
        String past_date = new SimpleDateFormat("yyyy/MM/dd").format(dateBefore);

        String todays_date = new SimpleDateFormat("yyyy/MM/dd").format(new Date());
        String filename = past_date + "_" + todays_date + "_grants.csv";

        HttpHeaders header = new HttpHeaders();
        header.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename="+filename);
        header.add("Cache-Control", "no-cache, no-store, must-revalidate");
        header.add("Pragma", "no-cache");
        header.add("Expires", "0");

        byte[] csvAsBytes = csv.getBytes();
        Resource resource = new ByteArrayResource(csvAsBytes);

        return ResponseEntity.ok()
                .headers(header)
                .contentLength(csvAsBytes.length)
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(resource);
    }

}
