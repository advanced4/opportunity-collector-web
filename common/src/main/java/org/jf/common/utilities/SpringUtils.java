package org.jf.common.utilities;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

// This class contains all the things that rely on the spring dependency
public class SpringUtils {
    private static final Logger log = LoggerFactory.getLogger(SpringUtils.class);

    @SuppressWarnings("unchecked")
    public static Jws<Claims> getTokenFromReqCTX() {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        assert attrs != null;

        Assert.state(attrs instanceof ServletRequestAttributes, "Missing ServletRequestAttributes");
        return (Jws<Claims>) attrs.getAttribute("token", RequestAttributes.SCOPE_REQUEST);
    }

    // Not used often, as usually we're overwriting a cookie which has the same effect.
    // Only used on logout, or if loading the login page with an expired token
    public static void clearCookie(HttpServletResponse httpServletResponse, String domain, String name, String path) {
        Cookie cookie = new Cookie(name, null);
        if(path == null) {
            cookie.setPath("/");
        }else{
            cookie.setPath(path);
        }
        cookie.setHttpOnly(true);
        cookie.setMaxAge(0); // expire right now
        if(!domain.equals("ignore")) {
            log.debug("(clearing) Setting domain to: " + domain);
            cookie.setDomain(domain); // visible only to domain
        }else{
            log.debug("ignoring cookie domain");
        }
        httpServletResponse.addCookie(cookie);
    }

    // set a cookie
    private static void setCookie(HttpServletResponse httpServletResponse, String name, String value, Boolean secure, String domain, String path, int expiration, boolean httponly) {
        Cookie cookie = new Cookie(name, value);
        cookie.setSecure(secure); // TLS only
        cookie.setHttpOnly(httponly); // invisible to javascript
        cookie.setMaxAge(expiration); // 0 = expire now, <0 = on browser exit (i.e. session)
        cookie.setPath("/"); // visible to all paths on domain

        if(path != null){
            cookie.setPath(path);
        }

        // If we only ever serve stuff on one domain, then this could permenantly be set to "ignore"
        if(!domain.equals("ignore")) {
            log.debug("(setting) Setting domain to: " + domain);
            cookie.setDomain(domain); // visible only to domain
        }else{
            log.debug("ignoring cookie domain");
        }

        httpServletResponse.addCookie(cookie);
    }

    public static void setJwtCookie(HttpServletResponse httpResponse, String cookieName, String token, Boolean secure, String domain){
        // this cookie can be seen by javascript, which is a security concern. but access tokens are less valuable than refresh tokens.
        // this tradeoff allows the client to read the -actual- expiry on the token, rather than some round-about way of storing the expiry
        // seperately, possible getting out of sync, missing,etc.
        setCookie(httpResponse, cookieName, token, secure, domain, null, -1, false);
    }

}
