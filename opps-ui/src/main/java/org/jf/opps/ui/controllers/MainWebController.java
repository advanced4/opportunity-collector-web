package org.jf.opps.ui.controllers;

import org.jf.common.constants.OppsConstants;
import org.jf.common.utilities.SpringUtils;
import org.jf.opps.ui.config.WebConfig;
import org.jf.opps.ui.utils.WebUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletResponse;

import static org.jf.common.utilities.SpringUtils.getTokenFromReqCTX;

@Controller
public class MainWebController {
    private static final Logger log  = LoggerFactory.getLogger(MainWebController.class);
    private WebConfig webConfig;

    MainWebController(WebConfig webConfig){
        this.webConfig = webConfig;
    }

    @RequestMapping(value = "/dashboard", method = RequestMethod.GET)
    public String dashboard( Model model) {
        WebUtils.addToCommonModel("Dashboard", model, webConfig, getTokenFromReqCTX());
        return "dashboard";
    }

    @RequestMapping(value = "/logout", method = RequestMethod.GET)
    public String logout(HttpServletResponse httpResponse, Model model) {
        WebUtils.addToCommonModel("Logout", model, webConfig, getTokenFromReqCTX());
        SpringUtils.clearCookie(httpResponse, webConfig.getMisc().getDomain(), OppsConstants.Jwt.JwtCookieNameOpps, null);
        return "redirect:/";
    }
}
