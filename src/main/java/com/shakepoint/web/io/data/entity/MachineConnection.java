package com.shakepoint.web.io.data.entity;

import javax.persistence.*;
import java.util.UUID;

@Entity(name = "MachineConnection")
@Table(name = "machine_connection")
public class MachineConnection {

    @Id
    private String id;

    @Column(name = "machine_id")
    private String machineId;

    @Column(name = "connection_active")
    private boolean connectionActive;

    @Column(name = "last_update")
    private String lastUpdate;

    @Column(name = "port")
    private int port;

    @Column(name = "token")
    private String machineToken;

    public MachineConnection() {
        id = UUID.randomUUID().toString();
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMachineId() {
        return machineId;
    }

    public void setMachineId(String machineId) {
        this.machineId = machineId;
    }

    public boolean isConnectionActive() {
        return connectionActive;
    }

    public void setConnectionActive(boolean connectionActive) {
        this.connectionActive = connectionActive;
    }

    public String getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(String lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public String getMachineToken() {
        return machineToken;
    }

    public void setMachineToken(String machineToken) {
        this.machineToken = machineToken;
    }
}
