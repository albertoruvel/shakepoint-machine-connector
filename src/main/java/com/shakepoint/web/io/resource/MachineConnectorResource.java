package com.shakepoint.web.io.resource;

import com.shakepoint.web.io.data.dto.NewMachineConnectionRequest;
import com.shakepoint.web.io.service.ConnectorService;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("machine")
public class MachineConnectorResource {

    @Inject
    private ConnectorService connectorService;


    @POST
    @Path("createMachineConnection")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createMachineConnection(NewMachineConnectionRequest request)throws Exception{
        return connectorService.createMachineConnection(request);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getMachineConnectionStatus(@QueryParam("connectionId") String connectionId)throws Exception{
        return connectorService.getMachineConnectionStatus(connectionId);
    }
}
