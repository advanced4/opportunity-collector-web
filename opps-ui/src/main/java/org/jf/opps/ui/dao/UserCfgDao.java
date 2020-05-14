package org.jf.opps.ui.dao;

import org.jf.common.constants.OppsConstants;
import org.jf.common.exceptions.OppsExceptions;
import org.jf.opps.ui.models.GrantsCfg;
import org.jf.opps.ui.models.SamCfg;
import com.google.gson.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.sql.SQLException;
import java.sql.Types;
import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

@Component
public class UserCfgDao {

    private static final Logger log = LoggerFactory.getLogger(UserCfgDao.class);

    public UserCfgDao(@Qualifier("oppsDb") DataSource dataSource){
        jdbcTemplate = new JdbcTemplate(dataSource);
    }
    private JdbcTemplate jdbcTemplate;

    private final RowMapper<SamCfg> sam_rm = (rs, i) ->{
        SamCfg cfg = new SamCfg();
        cfg.setSam_api_key_last_change_ms(rs.getLong("sam_api_key_last_change_ms"));
        cfg.setSam_naics(rs.getString("sam_naics_json"));
        cfg.setSam_types(rs.getString("sam_types_json"));
        cfg.setLast_search_date(rs.getLong("sam_last_search_ms"));

        try {
            cfg.setSam_api_key(rs.getString("sam_api_key"));
        }catch(SQLException ignored){
            // not found. we only sometimes need the api key.
            // other times we don't query for it, because we're just getting the user cfg for the UI
            // which should -not- contain the api key
        }

        return cfg;
    };

    private final RowMapper<GrantsCfg> grants_rm = (rs, i) ->{
        GrantsCfg cfg = new GrantsCfg();
        cfg.setCats(rs.getString("grants_cats_json"));
        cfg.setEligibilities(rs.getString("grants_eligibilities_json"));
        cfg.setInstruments(rs.getString("grants_instruments_json"));
        cfg.setLast_search_date(rs.getLong("grants_last_search_ms"));

        return cfg;
    };

    public GrantsCfg getGrantsCfg(UUID uid){
        String sql = "select grants_cats_json, grants_eligibilities_json, grants_instruments_json, grants_last_search_ms FROM cfg WHERE uid=?";

        PreparedStatementSetter pss = ps ->{
            ps.setObject(1, uid, Types.VARCHAR);
        };

        List<GrantsCfg> result = jdbcTemplate.query(sql, pss, grants_rm);
        if (result.isEmpty()) {
            return null;
        }

        return result.get(0);
    }

    public boolean setGrantsCfgOnQuery(UUID uid, JsonArray inst, JsonArray elig, JsonArray cats) throws OppsExceptions.BadGrantsCfgException {
        for(JsonElement je : elig) {
            if (!OppsConstants.Grants.eligibilities_abv_plain.containsKey(je.getAsString())) {
                throw new OppsExceptions.BadGrantsCfgException("bad eligibility abv");
            }
        }

        for(JsonElement je : cats) {
            if (!OppsConstants.Grants.cats_abv_plain.containsKey(je.getAsString())) {
                throw new OppsExceptions.BadGrantsCfgException("bad cat abv");
            }
        }

        for(JsonElement je : inst) {
            if (!OppsConstants.Grants.instruments_abv_plain.containsKey(je.getAsString())) {
                throw new OppsExceptions.BadGrantsCfgException("bad instrument abv");
            }
        }

        String sql = "UPDATE cfg SET grants_last_search_ms=?,grants_eligibilities_json=?,grants_cats_json=?,grants_instruments_json=? WHERE uid=?";

        PreparedStatementSetter pss = ps ->{
            ps.setLong(1, System.currentTimeMillis());
            ps.setString(2, elig.toString());
            ps.setString(3, cats.toString());
            ps.setString(4, inst.toString());
            ps.setObject(5, uid, Types.VARCHAR);
        };

        int res = jdbcTemplate.update(sql, pss);

        if(res < 1 ){
            log.error("Failed to update grants last search timestamp");
            return false;
        }
        return true;
    }

    public boolean setSamCfgOnQuery(UUID uid, JsonArray types, JsonArray naics) throws OppsExceptions.BadSamCfgException {
        for(JsonElement je : types) {
            if (!OppsConstants.Sam.types_abv_plain.containsKey(je.getAsString())) {
                throw new OppsExceptions.BadSamCfgException("Bad SAM Type");
            }
        }

        String sql = "UPDATE cfg SET sam_types_json=?,sam_naics_json=?,sam_last_search_ms=? WHERE uid=?";

        PreparedStatementSetter pss = ps ->{
            ps.setString(1, types.toString());
            ps.setString(2, naics.toString());
            ps.setLong(3, System.currentTimeMillis());
            ps.setObject(4, uid, Types.VARCHAR);
        };

        int res = jdbcTemplate.update(sql, pss);

        if(res < 1 ){
            log.error("Failed to update sam cfg");
            return false;
        }
        return true;
    }

    public SamCfg getSamCfg(UUID uid){
        String sql = "select sam_types_json,sam_naics_json,sam_api_key_last_change_ms,sam_last_search_ms FROM cfg WHERE uid=?";

        PreparedStatementSetter pss = ps ->{
            ps.setObject(1, uid, Types.VARCHAR);
        };

        List<SamCfg> result = jdbcTemplate.query(sql, pss, sam_rm);
        if (result.isEmpty()) {
            return null;
        }

        return result.get(0);
    }

    public SamCfg getSamCfgWithKey(UUID uid){
        String sql = "select sam_types_json,sam_naics_json,sam_api_key_last_change_ms,sam_last_search_ms,sam_api_key FROM cfg WHERE uid=?";

        PreparedStatementSetter pss = ps ->{
            ps.setObject(1, uid, Types.VARCHAR);
        };

        List<SamCfg> result = jdbcTemplate.query(sql, pss, sam_rm);
        if (result.isEmpty()) {
            return null;
        }

        return result.get(0);
    }

    public boolean updateSamKey(UUID uid, String key){
        String sql = "UPDATE cfg SET sam_api_key=?, sam_api_key_last_change_ms=? WHERE uid=?";

        PreparedStatementSetter pss = ps ->{
            ps.setString(1, key);
            ps.setLong(2, System.currentTimeMillis()/1000);
            ps.setObject(3, uid, Types.VARCHAR);
        };

        int res = jdbcTemplate.update(sql, pss);

        if(res < 1 ){
            log.error("Failed to update sam API key");
            return false;
        }
        return true;
    }

     public boolean insertNewUser(UUID uid){
        String sql = "INSERT INTO cfg (uid, sam_types_json, sam_naics_json, grants_cats_json, grants_eligibilities_json, grants_instruments_json) VALUES (?, ?, ?, ?, ?, ?)";

        // this is mostly just random stuff just to initalize a users cfg.
        // the reasoning is: the UI may be more intuitive if there's stuff already there so you see how its
         // supposed to look
        JsonArray sam_types = new JsonArray();
        for(String type : OppsConstants.Sam.types_abv_plain.keySet()){
            if(Math.random() < 0.5){ // flip a coin
                sam_types.add(type);
            }
        }

         JsonArray sam_naics = new JsonArray();
         JsonObject naics_entry1 = new JsonObject();
         naics_entry1.addProperty("code","511210");
         naics_entry1.addProperty("desc","Software Publishers");

         JsonObject naics_entry2 = new JsonObject();
         naics_entry2.addProperty("code","541990");
         naics_entry2.addProperty("desc","All Other Professional, Scientific, and Technical Services");

         sam_naics.add(naics_entry1);
         sam_naics.add(naics_entry2);

         JsonArray grants_cat = new JsonArray();
         // for each cfg item, pick some random items to initalize it with
         for(String type : OppsConstants.Grants.cats_abv_plain.keySet()){
             if(Math.random() < 0.5){ // flip a coin
                 grants_cat.add(type);
             }
         }
         // of course because its random, its possible that none are chosen >_>
         if(grants_cat.size() == 0){
             grants_cat.add(OppsConstants.Grants.cats_abv_plain.keySet().iterator().next());
         }

         JsonArray grants_elig = new JsonArray();
         for(String type : OppsConstants.Grants.eligibilities_abv_plain.keySet()){
             if(Math.random() < 0.5){ // flip a coin
                 grants_elig.add(type);
             }
         }
         if(grants_elig.size() == 0){
             grants_elig.add(OppsConstants.Grants.eligibilities_abv_plain.keySet().iterator().next());
         }

         JsonArray grants_inst = new JsonArray();
         for(String type : OppsConstants.Grants.instruments_abv_plain.keySet()){
             if(Math.random() < 0.5){ // flip a coin
                 grants_inst.add(type);
             }
         }
         if(grants_inst.size() == 0){
             grants_inst.add(OppsConstants.Grants.instruments_abv_plain.keySet().iterator().next());
         }

        PreparedStatementSetter pss = ps ->{
            ps.setObject(1, uid, Types.VARCHAR);
            ps.setString(2, sam_types.toString());
            ps.setString(3, sam_naics.toString());

            ps.setString(4, grants_cat.toString());
            ps.setString(5, grants_elig.toString());
            ps.setString(6, grants_inst.toString());
        };

        int res;
        try {
            res = jdbcTemplate.update(sql, pss);
        }catch(DuplicateKeyException dke){
            throw new DuplicateKeyException("Entry already exists");
        }

        if(res < 1 ){
            log.error("Failed to insert new user cfg");
            return false;
        }
        return true;
    }

    public boolean removeCfg(UUID uid){
        String sql = "DELETE FROM cfg WHERE uid=?";

        PreparedStatementSetter pss = ps -> {
            ps.setObject(1, uid, Types.VARCHAR);
        };

        int res = jdbcTemplate.update(sql, pss);

        if(res < 1 ){
            log.error("Failed to delete user cfg");
            return false;
        }
        return true;
    }

}
