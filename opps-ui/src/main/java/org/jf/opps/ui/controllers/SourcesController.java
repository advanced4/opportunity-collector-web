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
public class SourcesController {
    private static final Logger log  = LoggerFactory.getLogger(SourcesController.class);
    private WebConfig webConfig;

    SourcesController(WebConfig webConfig){
        this.webConfig = webConfig;
    }

    @RequestMapping(value = "sam", method = RequestMethod.GET)
    public String sam(Model model) {
        Jws<Claims> token = getTokenFromReqCTX();
        WebUtils.addToCommonModel("Sam.gov", model, webConfig, token);

        model.addAttribute("types", OppsConstants.Sam.types_abv_plain);
        return "sam";
    }

    @RequestMapping(value = "grants", method = RequestMethod.GET)
    public String grants(Model model) {
        Jws<Claims> token = getTokenFromReqCTX();
        WebUtils.addToCommonModel("Grants.gov", model, webConfig, token);

        model.addAttribute("eligs", OppsConstants.Grants.eligibilities_abv_plain);
        model.addAttribute("cats", OppsConstants.Grants.cats_abv_plain);
        model.addAttribute("insts", OppsConstants.Grants.instruments_abv_plain);
        return "grants";
    }

}
