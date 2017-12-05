package com.shakepoint.web.io.resource;

import com.shakepoint.web.io.service.ConnectorService;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("machine")
public class MachineConnectorResource {

    @Inject
    private ConnectorService connectorService;

    @GET
    @Path("getMachineStatus")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getMachineConnectionStatus(@QueryParam("connectionId") String connectionId)throws Exception{
        return connectorService.getMachineConnectionStatus(connectionId);
    }

}
