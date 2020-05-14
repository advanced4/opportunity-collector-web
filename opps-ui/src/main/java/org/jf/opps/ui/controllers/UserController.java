package org.jf.opps.ui.controllers;

import org.jf.common.constants.OppsConstants;
import org.jf.opps.ui.config.WebConfig;
import org.jf.opps.ui.utils.WebUtils;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import static org.jf.common.utilities.SpringUtils.getTokenFromReqCTX;

@Controller
@RequestMapping("/user/")
public class UserController {
    private static final Logger log  = LoggerFactory.getLogger(UserController.class);
    private WebConfig webConfig;

    UserController(WebConfig webConfig){
        this.webConfig = webConfig;
    }

    @RequestMapping(value = "list", method = RequestMethod.GET)
    public String listUsers(Model model) {
        Jws<Claims> token = getTokenFromReqCTX();
        if(!WebUtils.isAdmin(token, webConfig)){
            model.addAttribute("error", OppsConstants.Strings.ERR_UNAUTHORIZED);
            return "dashboard";
        }

        WebUtils.addToCommonModel("Users", model, webConfig, token);

        return "listusers";
    }

    @RequestMapping(value = "register", method = RequestMethod.GET)
    public String registerUser(Model model) {
        Jws<Claims> token = getTokenFromReqCTX();
        if(!WebUtils.isAdmin(token, webConfig)){
            model.addAttribute("error", OppsConstants.Strings.ERR_UNAUTHORIZED);
            return "dashboard";
        }

        WebUtils.addToCommonModel("Register", model, webConfig, token);
        return "register";
    }

    @RequestMapping(value = "changeotherpw", method = RequestMethod.GET)
    public String changeOtherPassword(Model model) {
        Jws<Claims> token = getTokenFromReqCTX();
        if(!WebUtils.isAdmin(token, webConfig)){
            model.addAttribute("error", OppsConstants.Strings.ERR_UNAUTHORIZED);
            return "dashboard";
        }

        WebUtils.addToCommonModel("Change Password", model, webConfig, token);

        return "changeotherpw";
    }

    @RequestMapping(value = "changepw", method = RequestMethod.GET)
    public String changeMyPassword(Model model) {
        Jws<Claims> token = getTokenFromReqCTX();
        WebUtils.addToCommonModel("Change Password", model, webConfig, token);
        return "changepw";
    }

}
