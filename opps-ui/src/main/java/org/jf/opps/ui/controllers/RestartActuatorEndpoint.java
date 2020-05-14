package org.jf.opps.ui.controllers;

import org.jf.opps.ui.SpringOppsUiMain;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;
import org.springframework.stereotype.Component;

@Component
@Endpoint(id="restart")
public class RestartActuatorEndpoint  {

    @ReadOperation
    public String notSupported(){
        return "not supported";
    }

    // aka POST
    @WriteOperation
    public String softRestart(){
        SpringOppsUiMain.restart();
        return "acknowledged"; // idk probably doesn't matter
    }

}
