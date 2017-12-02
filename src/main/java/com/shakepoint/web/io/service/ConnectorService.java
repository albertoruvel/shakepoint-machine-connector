package com.shakepoint.web.io.service;

import com.shakepoint.web.io.data.dto.req.NewMachineConnectionRequest;

import javax.ws.rs.core.Response;

public interface ConnectorService {
    public Response createMachineConnection(NewMachineConnectionRequest request);

    public Response getMachineConnectionStatus(String connectionId)throws Exception;

}
