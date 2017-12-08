package com.shakepoint.web.io.service;

import javax.ws.rs.core.Response;

public interface ConnectorService {
    public Response getMachineConnectionStatus(String connectionId)throws Exception;
    public void createConnection(String machineId)throws InterruptedException;
}
