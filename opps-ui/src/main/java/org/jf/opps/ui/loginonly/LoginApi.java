package org.jf.opps.ui.loginonly;

import org.jf.common.models.Response;
import org.jf.common.models.User;
import org.jf.common.utilities.BCrypt;
import org.jf.common.utilities.SpringUtils;
import org.jf.common.utilities.Utils;
import org.jf.opps.ui.config.WebConfig;
import org.jf.opps.ui.dao.BruteForceDao;
import org.jf.opps.ui.dao.UserAccountsDao;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.security.KeyPair;

import static org.jf.opps.ui.loginonly.LoginController.createJwt;


@RestController
@Api(value = "Login API")
@RequestMapping("/login/api/")
public class LoginApi {
    private static final Logger log = LoggerFactory.getLogger(LoginApi.class);
    private WebConfig webConfig;
    private KeyPair jwtkeys;
    private UserAccountsDao uad;
    private BruteForceDao bfd;

    // spring does this
    LoginApi(UserAccountsDao uad, WebConfig webConfig, BruteForceDao bfd, KeyPair jwtkeys) {
        this.uad = uad;
        this.bfd = bfd;
        this.webConfig = webConfig;
        this.jwtkeys = jwtkeys;
    }

    private static class TokenResponse extends Response.StandardResponse{
        public TokenResponse() {super();}

        private String token;

        public String getToken() {return token;}
        public void setToken(String token) {this.token = token;}
    }

    @RequestMapping(value = "login", method = {RequestMethod.POST}, produces= MediaType.APPLICATION_JSON_VALUE)
    private TokenResponse apiLogin(HttpServletResponse httpRes,
                                   @ApiParam(type="query", value="The account email", required=true)String email,
                                   @ApiParam(type="query", value="The SHA512(yourpassword)", required=true)String password) {

        TokenResponse res = new TokenResponse();

        Jws<Claims> parsedToken = SpringUtils.getTokenFromReqCTX();
        // this should never happen, because if they had a valid token, their initial GET /login should have redirected them to /main
        // but it doesn't hurt to have it here
        if (Utils.checkToken_b(parsedToken)) {
            // if not null, we have a valid token
            // We're logged in
            res.setStatus(Response.Status.ok);
            res.setMsg("You're already logged in");
            return res;
        }

        // Make sure we got the arguments we want
        if (Utils.isAnyNullOrEmpty(email, password)) {
            res.setMsg("Missing email or password");
            return res;
        }

        if(password.length() != 128){
            res.setMsg("Bad sha512 hash");
            return res;
        }

        // check if username is reasonable
        if(email.length() < 2 || email.length() > 64){
            res.setMsg("bad email length");
            return res;
        }

        try {
            User user = uad.findByEmailFullLogin(email);

            // check for bruteforce
            if (bfd.checkBruteForce(user.getId())) {
                log.warn("Bruteforce detected on user: " + email);
                httpRes.setStatus(423);
                res.setMsg("Account locked. Too many failed login attempts");
                return res;
            }

            if(BCrypt.checkpw(password, user.getPassword())){
                String nt = createJwt(email, user.getId(), webConfig.getJwt(), jwtkeys.getPrivate(), user.isAdmin(), user.getUsername());
                res.setStatus(Response.Status.ok);
                res.setToken(nt);
                return res;
            } else {
                log.debug("user {} had wrong password", email);
                res.setMsg("bad username or password");
                return res;
            }

        }catch(UserAccountsDao.UserNotFoundException e) {
            log.debug("username not found");
            res.setMsg("bad username or password");
            return res;
        }
    }

}
