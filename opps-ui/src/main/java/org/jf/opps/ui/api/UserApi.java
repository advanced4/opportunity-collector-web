package org.jf.opps.ui.api;

import org.jf.common.constants.OppsConstants;
import org.jf.common.models.Response;
import org.jf.common.models.Response.StandardResponse;
import org.jf.common.models.Response.Status;
import org.jf.common.models.User;
import org.jf.common.utilities.BCrypt;
import org.jf.common.utilities.Utils;
import org.jf.opps.ui.config.WebConfig;
import org.jf.opps.ui.dao.UserAccountsDao;
import org.jf.opps.ui.dao.UserCfgDao;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiParam;
import org.jf.opps.ui.utils.WebUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.naming.AuthenticationException;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.UUID;

import static org.jf.common.utilities.SpringUtils.getTokenFromReqCTX;

@RestController
@Api(value = "User API")
@RequestMapping("/user/api/")
public class UserApi {
    private static final Logger log = LoggerFactory.getLogger(UserApi.class);
    private WebConfig webConfig;
    private UserAccountsDao uad;
    private UserCfgDao ucd;

    // spring does this
    UserApi(UserAccountsDao uad, WebConfig webConfig, UserCfgDao ucd) {
        this.uad = uad;
        this.ucd = ucd;
        this.webConfig = webConfig;
    }

    private static class UsersResponse extends Response.StandardResponse{
        public UsersResponse() {super();}

        private JsonArray cpw;
        public JsonArray getCpw() {return cpw;}
        public void setCpw(JsonArray cpw) {this.cpw = cpw;}

        private List<User> admins;
        public List<User> getAdmins() {return admins;}
        public void setAdmins(List<User> admins) {this.admins = admins;}

        private List<User> users;
        public List<User> getUsers() {return users;}
        public void setUsers(List<User> users) {this.users = users;}
    }


    @RequestMapping(value = "users", method = {RequestMethod.GET}, produces=MediaType.APPLICATION_JSON_VALUE)
    public UsersResponse getUsers(HttpServletResponse httpResponse) {
        UsersResponse res = new UsersResponse();

        if(!WebUtils.isAdmin(getTokenFromReqCTX(), webConfig)){
            res.setMsg(OppsConstants.Strings.ERR_UNAUTHORIZED);
            httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return res;
        }

        List<User> users = uad.getNonAdmins();
        List<User> adms = uad.getAdmins();

        res.setStatus(Status.ok);
        res.setUsers(users);
        res.setAdmins(adms);

        return res;
    }


    @RequestMapping(value = "cpwusers", method = {RequestMethod.GET}, produces=MediaType.APPLICATION_JSON_VALUE)
    public UsersResponse getChangeOtherPwdUsers(HttpServletResponse httpResponse) {
        UsersResponse res = new UsersResponse();

        Jws<Claims> token = getTokenFromReqCTX();
        if(!WebUtils.isAdmin(token, webConfig)){
            res.setMsg(OppsConstants.Strings.ERR_UNAUTHORIZED);
            httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return res;
        }

        // plz don't change admin passwords thx
        List<User> users = uad.getNonAdmins();

        JsonArray ja = new JsonArray();
        for(User u : users){
            JsonObject jo = new JsonObject();
            jo.addProperty("username", u.getUsername());
            jo.addProperty("uid", u.getId().toString());
            ja.add(jo);
        }

        res.setCpw(ja);
        res.setStatus(Status.ok);

        return res;
    }

    @RequestMapping(value = "{id}", method = {RequestMethod.DELETE}, produces=MediaType.APPLICATION_JSON_VALUE)
    public StandardResponse deleteUser(HttpServletResponse httpResponse, @PathVariable("id") UUID uid) throws AuthenticationException {
        StandardResponse res = new StandardResponse();

        if(!WebUtils.isAdmin(getTokenFromReqCTX(), webConfig)){
            res.setMsg(OppsConstants.Strings.ERR_UNAUTHORIZED);
            httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return res;
        }

        if(Utils.isAnyNullOrEmpty(uid)){
            res.setMsg(OppsConstants.Strings.ERR_MISSING_PARAMS);
            httpResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return res;
        }

        if(uad.deleteUser(uid)){
            ucd.removeCfg(uid);
            res.setStatus(Status.ok);
            return res;
        }else{
            res.setMsg("Error deleting user");
            httpResponse.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return res;
        }
    }

    @RequestMapping(value = "changepw", method = {RequestMethod.PATCH}, produces=MediaType.APPLICATION_JSON_VALUE)
    public StandardResponse changeMyPassword(HttpServletResponse httpResponse,
                                             @ApiParam(type="query", value="SHA512 of password", required=true)String p) {
        StandardResponse res = new StandardResponse();

        if(Utils.isAnyNullOrEmpty(p)){
            res.setMsg(OppsConstants.Strings.ERR_MISSING_PARAMS);
            httpResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return res;
        }

        if(p.length() != 128){
            res.setMsg("Password should be in the form of a SHA512 hash");
            httpResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return res;
        }

        Jws<Claims> token = getTokenFromReqCTX();
        UUID id = UUID.fromString(token.getBody().get(OppsConstants.Jwt.JwtUid, String.class));
        if(uad.bcryptAndChangePassword(id, p)){
            res.setStatus(Status.ok);
        }else{
            res.setMsg("Failed to update password");
        }

        return res;
    }

    @RequestMapping(value = "changeotherpw", method = {RequestMethod.PATCH}, produces=MediaType.APPLICATION_JSON_VALUE)
    public StandardResponse changeOtherPassword(HttpServletResponse httpResponse,
                                                @ApiParam(type="query", value="SHA512 of password", required=true)String p,
                                                @ApiParam(type="query", value="ID of user to change PW of", required=true)String id) {
        StandardResponse res = new StandardResponse();

        if(!WebUtils.isAdmin(getTokenFromReqCTX(), webConfig)){
            res.setMsg(OppsConstants.Strings.ERR_UNAUTHORIZED);
            httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return res;
        }

        if(Utils.isAnyNullOrEmpty(p, id)){
            res.setMsg(OppsConstants.Strings.ERR_MISSING_PARAMS);
            httpResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return res;
        }

        if(p.length() != 128){
            res.setMsg("Password should be in the form of a SHA512 hash");
            httpResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return res;
        }
        UUID otheruserId;
        try {
            otheruserId = UUID.fromString(id);
        }catch(IllegalArgumentException iae){
            res.setMsg(OppsConstants.Strings.ERR_INVALID_PARAMS);
            httpResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return res;
        }


        // Don't fuck with devs plsthx
        if(uad.isAdmin(otheruserId)){
            res.setMsg(OppsConstants.Strings.ERR_UNAUTHORIZED);
            httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return res;
        }

        if(uad.bcryptAndChangePassword(otheruserId, p)){
            res.setStatus(Status.ok);
        }else{
            res.setMsg("Failed to update password");
        }

        return res;
    }

    @RequestMapping(value = "enable", method = {RequestMethod.PATCH}, produces=MediaType.APPLICATION_JSON_VALUE)
    public StandardResponse enableAccount(HttpServletResponse httpResponse, @ApiParam(type="query", value="ID of user to disable", required=true)String id) {
        StandardResponse res = new StandardResponse();

        if(!WebUtils.isAdmin(getTokenFromReqCTX(), webConfig)){
            res.setMsg(OppsConstants.Strings.ERR_UNAUTHORIZED);
            httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return res;
        }

        if(Utils.isAnyNullOrEmpty( id)){
            res.setMsg(OppsConstants.Strings.ERR_MISSING_PARAMS);
            httpResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return res;
        }

        UUID otheruserId;
        try {
            otheruserId = UUID.fromString(id);
        }catch(IllegalArgumentException iae){
            res.setMsg(OppsConstants.Strings.ERR_INVALID_PARAMS);
            httpResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return res;
        }

        if(uad.enableUser(otheruserId)){
            res.setStatus(Status.ok);
        }else{
            res.setMsg("Failed to enable user");
        }

        return res;
    }

    @RequestMapping(value = "disable", method = {RequestMethod.PATCH}, produces=MediaType.APPLICATION_JSON_VALUE)
    public StandardResponse disableAccount(HttpServletResponse httpResponse, @ApiParam(type="query", value="ID of user to disable", required=true)String id) {
        StandardResponse res = new StandardResponse();

        if(!WebUtils.isAdmin(getTokenFromReqCTX(), webConfig)){
            res.setMsg(OppsConstants.Strings.ERR_UNAUTHORIZED);
            httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return res;
        }

        if(Utils.isAnyNullOrEmpty( id)){
            res.setMsg(OppsConstants.Strings.ERR_MISSING_PARAMS);
            httpResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return res;
        }

        UUID otheruserId;
        try {
            otheruserId = UUID.fromString(id);
        }catch(IllegalArgumentException iae){
            res.setMsg(OppsConstants.Strings.ERR_INVALID_PARAMS);
            httpResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return res;
        }

        // Don't fuck with devs plsthx
        if(uad.isAdmin(otheruserId)){
            res.setMsg(OppsConstants.Strings.ERR_UNAUTHORIZED);
            httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return res;
        }

        if(uad.disableUser(otheruserId)){
            res.setStatus(Status.ok);
        }else{
            res.setMsg("Failed to disable user");
        }

        return res;
    }

    @RequestMapping(value = "create", method = {RequestMethod.POST}, produces=MediaType.APPLICATION_JSON_VALUE)
    public StandardResponse createuser(HttpServletResponse httpResponse,
                                       @ApiParam(type="query", value="Username", required=true)String username,
                                       @ApiParam(type="query", value="Email Address", required=true)String email,
                                       @ApiParam(type="query", value="SHA512 of password", required=true)String p){
        StandardResponse res = new StandardResponse();

        if(!WebUtils.isAdmin(getTokenFromReqCTX(), webConfig)){
            res.setMsg(OppsConstants.Strings.ERR_UNAUTHORIZED);
            httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return res;
        }

        log.debug("got params: username={}, email={}, p={}", username, email, p);

        if(Utils.isAnyNullOrEmpty(username, email, p)){
            res.setMsg(OppsConstants.Strings.ERR_MISSING_PARAMS);
            httpResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return res;
        }

        if (username != null && (username.length() < 3 || username.length() > 64)) {
            res.setMsg("Invalid username length");
            httpResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return res;
        }

        if (email != null && (email.length() < 5 || email.length() > 128 || !email.contains("@") || !email.contains("."))){
            res.setMsg("Invalid email");
            httpResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return res;
        }

        if(p.length() != 128){
            res.setMsg("Invalid password");
            httpResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return res;
        }

        if (uad.userExistsByEmail(email)) {
            res.setMsg("User already exists");
            httpResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return res;
        }

        // Then insert the new user
        UUID newUserID = UUID.randomUUID();
        if(uad.insertNewUser(newUserID, username, email, BCrypt.hashpw(p))){
            if(ucd.insertNewUser(newUserID)) { // also make an entry in the cfg table
                res.setStatus(Status.ok);
            }else{
                // if we failed making the cfg, delete the user
                uad.deleteUser(newUserID);
                res.setMsg("Failed to create default config for new user");
                httpResponse.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        }else{
            res.setMsg("Failed to add new user");
            httpResponse.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
        return res;
    }
}
