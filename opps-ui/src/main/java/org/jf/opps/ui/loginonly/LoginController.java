package org.jf.opps.ui.loginonly;

import org.jf.common.constants.OppsConstants;
import org.jf.common.models.User;
import org.jf.common.utilities.BCrypt;
import org.jf.common.utilities.SpringUtils;
import org.jf.common.utilities.Utils;
import org.jf.opps.ui.config.WebConfig;
import org.jf.opps.ui.dao.BruteForceDao;
import org.jf.opps.ui.dao.UserAccountsDao;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletResponse;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.util.Date;
import java.util.UUID;

@Controller
public class LoginController {
    private static final Logger log  = LoggerFactory.getLogger(LoginController.class);
    private WebConfig webConfig;
    private KeyPair jwtkeys;
    private UserAccountsDao uad;
    private BruteForceDao bfd;

    LoginController(WebConfig webConfig, KeyPair jwtkeys, UserAccountsDao uad, BruteForceDao bfd){
        this.webConfig = webConfig;
        this.jwtkeys = jwtkeys;
        this.uad = uad;
        this.bfd = bfd;

    }

    @RequestMapping(value = "/", method = RequestMethod.GET)
    public String home() {
        return "redirect:/login";
    }

    @RequestMapping(value = "/login", method = RequestMethod.GET)
    private String loginGet(Model model, @CookieValue(value = OppsConstants.Jwt.JwtCookieNameOpps, defaultValue = "") String rawToken,
                            HttpServletResponse httpResponse) {


        model.addAttribute("cname", webConfig.getMisc().getCname());
        // Lets check if we're logged in. If so, add some things to the model
        if(!rawToken.equals("")) {
            Jws<Claims> parsedToken = Utils.checkToken(rawToken, jwtkeys.getPublic());
            if (Utils.checkToken_b(parsedToken)) {
                // if not null, we have a valid token
                // We're logged in
                return "redirect:/dashboard";
            }else{
                // we got *something* but it was invalid, so lets clear it so we don't check a bad token twice
                SpringUtils.clearCookie(httpResponse, webConfig.getMisc().getDomain(), OppsConstants.Jwt.JwtCookieNameOpps, null);
            }
        }

        return "login";
    }

    @RequestMapping(value = "/login", method = RequestMethod.POST)
    private String loginPost(Model model, HttpServletResponse httpResponse, @CookieValue(value = OppsConstants.Jwt.JwtCookieNameOpps, defaultValue = "") String rawToken,
                             String email, String password) {

        // this should never happen, because if they had a valid token, their initial GET /login should have redirected them to /main
        // but it doesn't hurt to have it here
        if(!rawToken.equals("")) {
            Jws<Claims> parsedToken = Utils.checkToken(rawToken, jwtkeys.getPublic());
            if (Utils.checkToken_b(parsedToken)) {
                // if not null, we have a valid token
                // We're logged in
                return "redirect:/dashboard";
            }
        }

        // Make sure we got the arguments we want
        if (Utils.isAnyNullOrEmpty(email, password)) {
            model.addAttribute("error", "Missing email or password");
            return "login";
        }

        if(password.length() != 128){
            model.addAttribute("error", "Bad sha512 hash");
            return "login";
        }

        // check if username is reasonable
        if(email.length() < 2 || email.length() > 64){
            model.addAttribute("error", "bad email length");
            return "login";
        }

        try {
            User user = uad.findByEmailFullLogin(email);

            // check for bruteforce
            if (bfd.checkBruteForce(user.getId())) {
                log.warn("Bruteforce detected on user: " + email);
                model.addAttribute("error", "Too many failed login attempts");
                return "login";
            }

            if(BCrypt.checkpw(password, user.getPassword())){
                String nt = createJwt(email, user.getId(), webConfig.getJwt(), jwtkeys.getPrivate(), user.isAdmin(), user.getUsername());
                SpringUtils.setJwtCookie(httpResponse, OppsConstants.Jwt.JwtCookieNameOpps, nt, webConfig.getMisc().isSecuressl(), webConfig.getMisc().getDomain() );
                return "redirect:/dashboard";
            } else {
                log.debug("user {} had wrong password", email);
                model.addAttribute("error", "bad username or password");
                return "login";
            }

        }catch(UserAccountsDao.UserNotFoundException e) {
            log.debug("username not found");
            model.addAttribute("error", "bad username or password");
            return "login";
        }

    }

    protected static String createJwt(String email, UUID uid, WebConfig.Jwt jwtconfig, PrivateKey jwt_privkey, boolean isadmin, String username) {
        JwtBuilder tmpJwt = Jwts.builder()
                .signWith(jwt_privkey)
                .setHeaderParam("typ", OppsConstants.Jwt.TOKEN_TYPE_JWT)
                .setIssuer(jwtconfig.getIssuer())
                .setAudience(jwtconfig.getAudience())
                .setSubject(email)
                .setExpiration(new Date(System.currentTimeMillis() + jwtconfig.getAt_expiry_ms()))
                .claim(OppsConstants.Jwt.JwtAdmin, isadmin)
                .claim(OppsConstants.Jwt.JwtUsername, username)
                .claim(OppsConstants.Jwt.JwtUid, uid);

        return tmpJwt.compact();
    }
}
