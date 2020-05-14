package org.jf.opps.ui.api;

import org.jf.common.constants.OppsConstants;
import org.jf.common.models.Response;
import org.jf.common.utilities.Utils;
import org.jf.opps.ui.config.WebConfig;
import org.jf.opps.ui.dao.UserCfgDao;
import org.jf.opps.ui.models.GrantsCfg;
import org.jf.opps.ui.models.SamCfg;
import org.jf.opps.ui.utils.WebUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.util.UUID;

import static org.jf.common.utilities.SpringUtils.getTokenFromReqCTX;


@RestController
@Api(value = "User preference/configuration")
@RequestMapping("/cfg/api/")
public class CfgApi {
    private static final Logger log = LoggerFactory.getLogger(CfgApi.class);
    private WebConfig webConfig;
    private UserCfgDao ucd;

    // spring does this
    CfgApi(UserCfgDao ucd, WebConfig webConfig) {
        this.ucd = ucd;
        this.webConfig = webConfig;
    }

    private static class SamCfgResponse extends Response.StandardResponse{
        public SamCfgResponse() {super();}

        private SamCfg samCfg;
        public SamCfg getSamCfg() {return samCfg;}
        public void setSamCfg(SamCfg samCfg) {this.samCfg = samCfg;}
    }

    private static class GrantsCfgResponse extends Response.StandardResponse{
        public GrantsCfgResponse() {super();}

        private GrantsCfg grantsCfg;
        public GrantsCfg getGrantsCfg() {return grantsCfg;}
        public void setGrantsCfg(GrantsCfg grantsCfg) {this.grantsCfg = grantsCfg;}
    }

    // (on client side) if sam key is set, then we can check the last changed date to determine whether
    // we alert the user. if it isn't set, then we'll ask the user to set things up
    @RequestMapping(value = "sam", method = {RequestMethod.GET}, produces= MediaType.APPLICATION_JSON_VALUE)
    public SamCfgResponse getSamCfg() {
        SamCfgResponse res = new SamCfgResponse();
        UUID uid = WebUtils.getUuid(getTokenFromReqCTX(), webConfig);
        res.setSamCfg(ucd.getSamCfg(uid));
        res.setStatus(Response.Status.ok);
        return res;
    }

    // take in key, validate, set it (and update last changed date)
    @RequestMapping(value = "sam/apikey", method = {RequestMethod.POST}, produces= MediaType.APPLICATION_JSON_VALUE)
    public Response.StandardResponse setSamApiKey(HttpServletResponse httpResponse,
            @ApiParam(type="query", value="sam.gov API key (should be 40 chars, alphanumeric)", required=true)String apikey) {

        Response.StandardResponse res = new Response.StandardResponse();

        if(Utils.isAnyNullOrEmpty(apikey)){
            res.setMsg(OppsConstants.Strings.ERR_MISSING_PARAMS);
            httpResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return res;
        }

        boolean hasNonAlpha = apikey.matches("^.*[^a-zA-Z0-9 ].*$");
        if(hasNonAlpha || apikey.length() != 40){
            res.setMsg("Bad API key");
            httpResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return res;
        }

        if(ucd.updateSamKey(WebUtils.getUuid(getTokenFromReqCTX(), webConfig), apikey)){
            res.setStatus(Response.Status.ok);
        }else{
            res.setMsg("failed to update API key");
        }

        return res;
    }

    @RequestMapping(value = "grants", method = {RequestMethod.GET}, produces= MediaType.APPLICATION_JSON_VALUE)
    public GrantsCfgResponse getGrantsCfg() {
        GrantsCfgResponse res = new GrantsCfgResponse();
        UUID uid = WebUtils.getUuid(getTokenFromReqCTX(), webConfig);
        res.setGrantsCfg(ucd.getGrantsCfg(uid));
        res.setStatus(Response.Status.ok);
        return res;
    }

}
