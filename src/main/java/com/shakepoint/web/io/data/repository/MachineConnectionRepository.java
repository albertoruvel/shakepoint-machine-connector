package com.shakepoint.web.io.data.repository;
import com.shakepoint.web.io.data.entity.MachineConnection;

import java.util.List;

public interface MachineConnectionRepository {
    public MachineConnection getConnection(String machineId);
    public MachineConnection getConnectionById(String id);
    public void createConnection(MachineConnection connection);

    public List<MachineConnection> getConnections();

    public boolean isConnectionActive(String id);

    public void updateConnection(MachineConnection connection);

    public void closeAllConnections();
}
