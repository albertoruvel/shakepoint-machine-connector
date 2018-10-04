package com.shakepoint.web.io.data.dto.res.socket;

public class ReplacementCheck {
    private Boolean machineUpdated;

    public ReplacementCheck(Boolean machineUpdated) {
        this.machineUpdated = machineUpdated;
    }

    public Boolean getMachineUpdated() {
        return machineUpdated;
    }

    public void setMachineUpdated(Boolean machineUpdated) {
        this.machineUpdated = machineUpdated;
    }
}
