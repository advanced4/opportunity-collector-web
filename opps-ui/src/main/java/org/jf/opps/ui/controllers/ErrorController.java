package org.jf.opps.ui.controllers;

import org.jf.opps.ui.config.WebConfig;
import org.jf.opps.ui.utils.WebUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.jf.common.utilities.SpringUtils.getTokenFromReqCTX;

@Controller
public class ErrorController implements org.springframework.boot.web.servlet.error.ErrorController {
    private static final Logger log = LoggerFactory.getLogger(ErrorController.class);

    private WebConfig webConfig;

    ErrorController(WebConfig webConfig){
        this.webConfig = webConfig;
    }

    @GetMapping("/error")
    public String errorHandler(HttpServletResponse res, HttpServletRequest req, Model model) throws IOException {
        return handleError(res, req, model, "get");
    }

    @PostMapping("/error")
    public String posterrorHandler(HttpServletResponse res, HttpServletRequest req, Model model) throws IOException {
        return handleError(res, req, model, "post");
    }

    private String handleError(HttpServletResponse res, HttpServletRequest req, Model model, String type) throws IOException {
        // Get status code to determine which view should be returned
        Integer statusCode = (Integer) req.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);

        if (statusCode == 401) {
            model.addAttribute("error", "Unauthorized.");
            model.addAttribute("cname", webConfig.getMisc().getCname());
            res.setStatus(statusCode);
            return "login";
        }

        WebUtils.addToCommonModel("Error", model, webConfig, getTokenFromReqCTX());
        model.addAttribute("errorcode", statusCode);

        if(statusCode.toString().startsWith("4")){
            model.addAttribute("color", "warning");
        }else{
            model.addAttribute("color", "danger");
        }

        if(statusCode == 404){
            model.addAttribute("msgshort", "Page not found.");
            model.addAttribute("msglong", "We could not find the page you were looking for.");
        }else{
            // this class is weird because this is the best way to handle 404 due to the way spring handles NoHandlerFoundException
            // I don't know what other errors will be caught here, but I *think* this class should only handle 404
            // and any other error should be handled in ExceptionHandlerAdvice
            log.error("!!!This shouldn't happen - error code {} is not handled properly", statusCode);
            model.addAttribute("msgshort", "An error occurred.");
            model.addAttribute("msglong", "We had a problem trying to serve this page.");
        }

        return "error";
    }

    public String getErrorPath() {
        return "/error";
    }
}
