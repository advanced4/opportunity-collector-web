package org.jf.opps.ui;

import org.jf.common.constants.OppsConstants;
import org.jf.common.utilities.SpringUtils;
import org.jf.common.utilities.Utils;
import org.jf.opps.ui.config.WebConfig;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.util.WebUtils;

import javax.servlet.*;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.jf.common.utilities.Utils.checkToken_b;

@Component
public class UrlFilter implements Filter {
    @Override
    public void destroy() {}

    private static final Logger log = LoggerFactory.getLogger(UrlFilter.class);

    private KeyPair jwtkeys;
    private WebConfig webConfig;
    private int MAX_REQUESTS_PER_MINUTE;

    private LoadingCache<String, Integer> requestCountsPerIpAddress;

    public UrlFilter(KeyPair jwtkeys, WebConfig webConfig){
        super();
        this.jwtkeys = jwtkeys;
        this.webConfig = webConfig;
        this.MAX_REQUESTS_PER_MINUTE = webConfig.getMisc().getMax_requests_per_minute();

        requestCountsPerIpAddress = CacheBuilder.newBuilder().
                expireAfterWrite(60, TimeUnit.SECONDS).build(new CacheLoader<String, Integer>() {
            public Integer load(String key) {
                return 0;
            }
        });
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterchain) throws IOException, ServletException {

        HttpServletResponse httpResponse = (HttpServletResponse) response;
        HttpServletRequest httpRequest = (HttpServletRequest) request;

        // lets see if we match if we match any, then let the user pass
        String requestUri = httpRequest.getRequestURI();
        String clientIpAddress = getClientIP(httpRequest);
        AntPathMatcher apm = new AntPathMatcher();

        // Logging in, actuator, and all API's are rate limited
        ArrayList<String> dosProtectedUrls = new ArrayList<>();
        dosProtectedUrls.add("/");
        dosProtectedUrls.add("/login");
        dosProtectedUrls.add("/**/api/**");
        dosProtectedUrls.add("/actuator**");

        for(String protectedUrl : dosProtectedUrls){
            if(apm.match(protectedUrl, requestUri)){
                if(isMaximumRequestsPerSecondExceeded(clientIpAddress)){
                    httpResponse.setStatus(420);
                    httpResponse.getWriter().write("Too many requests. Enhance your calm");
                    return;
                }
            }
        }

        httpResponse.setHeader("Access-Control-Allow-Credentials", "true");
        httpResponse.setHeader("Access-Control-Allow-Origin", Utils.filterOriginCORS(httpRequest.getHeader("Origin")));

        // anyone can send an OPTIONS request. browsers often do this as a "pre-flight" check
        if(HttpMethod.OPTIONS.matches(httpRequest.getMethod()) || HttpMethod.HEAD.matches(httpRequest.getMethod())){
            httpResponse.setHeader("Access-Control-Allow-Headers", OppsConstants.WebStuffs.ACCESS_CONTROL_ALLOW_HEADERS);
            httpResponse.setHeader("Access-Control-Allow-Methods","POST,GET,DELETE,PUT,PATCH");
            httpResponse.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        if(webConfig.getMisc().getDisableloginfordevelopment()){
            filterchain.doFilter(httpRequest, httpResponse);
            return;
        }

        // Anything in these directories does not require a login
        ArrayList<String> excludedDirectories = new ArrayList<>();

        // these are just static ui related files
        excludedDirectories.add("/js/**");
        excludedDirectories.add("/css/**");
        excludedDirectories.add("/img/**");
        excludedDirectories.add("/plugins/**");
        excludedDirectories.add("/fonts/**");
        excludedDirectories.add("/favicon.ico");

        // special cases
        excludedDirectories.add("/login");
        excludedDirectories.add("/login/api/login");
        excludedDirectories.add("/"); // just redirects to /login

        // API documentation - swagger docs
        excludedDirectories.add("/swagger-ui.html");
        excludedDirectories.add("/v2/**");
        excludedDirectories.add("/swagger-resources/**");
        excludedDirectories.add("/webjars/springfox-swagger-ui/**");


        for(String pattern : excludedDirectories){
            if(apm.match(pattern, requestUri)) {
                filterchain.doFilter(httpRequest, httpResponse);
                return;
            }
        }

        // special case for securing /actuator** && /actuator/** endpoints w/ basic auth only
        if(apm.match("/actuator**", requestUri) || apm.match("/actuator/**", requestUri)){
            final String authorization = httpRequest.getHeader("Authorization");
            if (authorization != null && authorization.toLowerCase().startsWith("basic")) {
                // Authorization: Basic base64credentials
                String base64Credentials = authorization.substring("Basic".length()).trim();
                byte[] credDecoded = Base64.getDecoder().decode(base64Credentials);
                String credentials = new String(credDecoded, StandardCharsets.UTF_8);
                // credentials = username:password
                final String[] values = credentials.split(":", 2);

                if(values.length < 2){
                    log.debug("invalid basic auth header. not enough parts");
                    httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    return;
                }

                if(webConfig.getMisc().getSpringadmin().getUsername().equals(values[0]) && webConfig.getMisc().getSpringadmin().getPassword().equals(values[1])){
                    filterchain.doFilter(httpRequest, httpResponse);
                    return;
                }else{
                    log.debug("unauthorized user requested actuator endpoint: {} from IP: {} with UA: {}", requestUri, httpRequest.getRemoteUser(), httpRequest.getHeader("User-Agent"));
                    log.debug("user provided bad username: {}, password: {}", values[0], values[1]);
                    httpResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                    return;
                }
            }else{
                log.debug("unauthorized user requested actuator endpoint: {} from IP: {} with UA: {}", requestUri, httpRequest.getRemoteUser(), httpRequest.getHeader("User-Agent"));
                httpResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
        }

        // Otherwise, If the user does have a token, lets check it, make it available, and allow the user to pass
        Cookie cookieToken = WebUtils.getCookie(httpRequest, OppsConstants.Jwt.JwtCookieNameOpps);
        String raw_authToken = httpRequest.getHeader(OppsConstants.WebStuffs.AUTH_HEADER);
        String raw_accessToken = Utils.chooseTokenFromHeaderOrCookie(raw_authToken, cookieToken == null ? null : cookieToken.getValue());
        Jws<Claims> parsedAccessToken = Utils.checkToken(raw_accessToken, jwtkeys.getPublic());
;
        // if we are logged in, then you may pass
        if (checkToken_b(parsedAccessToken)) {
            // But first lets make sure your token isn't about to expire. If it is, lets refresh it for you
            // update their existing access token with a new expiry

            Date expiration = parsedAccessToken.getBody().getExpiration();
            Date now = new Date();
            long difference = (expiration.getTime() - now.getTime());

            // Are we getting close to expiry? If not, just return
            if(difference <= OppsConstants.LoginSec.time_left_before_token_refresh_ms){
                String updatedToken = Jwts.builder()
                        .signWith(jwtkeys.getPrivate())
                        .setHeaderParam("typ", OppsConstants.Jwt.TOKEN_TYPE_JWT)
                        .setClaims(parsedAccessToken.getBody()) // must do this before re-setting the expiration
                        .setIssuer(webConfig.getJwt().getIssuer())
                        .setAudience(webConfig.getJwt().getAudience())
                        .setSubject(parsedAccessToken.getBody().getSubject())
                        .setExpiration(new Date(System.currentTimeMillis() + webConfig.getJwt().getAt_expiry_ms()))
                        .compact();
                SpringUtils.setJwtCookie(httpResponse, OppsConstants.Jwt.JwtCookieNameOpps, updatedToken, webConfig.getMisc().isSecuressl(), webConfig.getMisc().getDomain() );
            }

            RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
            assert attrs != null;
            attrs.setAttribute("token", parsedAccessToken, RequestAttributes.SCOPE_REQUEST);
            filterchain.doFilter(httpRequest, httpResponse);
            return;
        }

        // else unauthorized
        log.debug("unauthorized user requested: {} from IP: {} with UA: {}", requestUri, httpRequest.getRemoteUser(), httpRequest.getHeader("User-Agent"));
        httpResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED);
    }

    private boolean isMaximumRequestsPerSecondExceeded(String clientIpAddress){
        int requests = 0;
        try {
            requests = requestCountsPerIpAddress.get(clientIpAddress);
            if(requests > MAX_REQUESTS_PER_MINUTE){
                requestCountsPerIpAddress.put(clientIpAddress, requests);
                return true;
            }
        } catch (ExecutionException e) {
            requests = 0;
        }
        requests++;
        requestCountsPerIpAddress.put(clientIpAddress, requests);
        return false;
    }

    public String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null){
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];  // voor als ie achter een proxy zit
    }

    @Override
    public void init(FilterConfig filterconfig){}

}