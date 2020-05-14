package org.jf.opps.ui.controllers;

import org.jf.opps.ui.config.WebConfig;
import org.jf.opps.ui.utils.WebUtils;
import com.google.gson.JsonParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ui.Model;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import javax.naming.AuthenticationException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.jf.common.utilities.SpringUtils.getTokenFromReqCTX;

@ControllerAdvice
public class ExceptionHandlerAdvice {
    private static final Logger log = LoggerFactory.getLogger(ExceptionHandlerAdvice.class);

    private WebConfig webConfig;

    ExceptionHandlerAdvice(WebConfig webConfig){
        this.webConfig = webConfig;
    }

    @ExceptionHandler(Exception.class)
    public String defaultErrorHandler(Model model, HttpServletResponse res, Exception ex){
        // I can't predict all exceptions that could be thrown, so this is a catch-all
        // As new exceptions are discovered, they should be caught here. It doesn't really matter,
        // though its just to make a pretty error page

        log.debug("!!! To maintainer of this program: you should write a code to handle this specific error");
        log.error("DefaultErrorHandler: {}", ex.getMessage());
        ex.printStackTrace();
        res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

        WebUtils.addToCommonModel("Error", model, webConfig, getTokenFromReqCTX());

        model.addAttribute("color", "danger");
        model.addAttribute("msgshort", "500");
        model.addAttribute("msglong", "An unknown error occurred");
        return "error";
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public String badmethod(Model model, HttpServletRequest req, HttpServletResponse res, Exception ex) {
        log.error("Bad method: {}", ex.getMessage());
        res.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        log.debug("user requested: {}", req.getRequestURI());

        WebUtils.addToCommonModel("Error", model, webConfig, getTokenFromReqCTX());

        model.addAttribute("color", "warning");
        model.addAttribute("msgshort", "405 - Method Not Supported");
        model.addAttribute("msglong", "You sent a request with an unsupported HTTP method.");
        return "error";
    }

    @ExceptionHandler(JsonParseException.class)
    public String badjson(Model model, HttpServletResponse res, Exception ex) {
        log.error("JsonParseExceptionHandler: {}", ex.getMessage());
        res.setStatus(HttpServletResponse.SC_BAD_REQUEST);

        WebUtils.addToCommonModel("Error", model, webConfig, getTokenFromReqCTX());

        model.addAttribute("color", "warning");
        model.addAttribute("msgshort", "400 - Bad JSON");
        model.addAttribute("msglong", "You supplied some invalid JSON.");
        return "error";
    }

    @ExceptionHandler(AuthenticationException.class)
    public String unauthorized(Model model, HttpServletResponse res, Exception ex) {
        log.error("AuthenticationExceptionHandler: {}", ex.getMessage());
        res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        model.addAttribute("error", "Unauthorized.");
        return "login";
    }

}
