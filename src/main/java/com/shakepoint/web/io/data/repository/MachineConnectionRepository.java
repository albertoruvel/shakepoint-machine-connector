package com.shakepoint.web.io.data.repository;
import com.shakepoint.web.io.data.dto.req.socket.QRCodeValidationMachineMessage;
import com.shakepoint.web.io.data.entity.MachineConnection;
import com.shakepoint.web.io.data.entity.Product;
import com.shakepoint.web.io.data.entity.Purchase;

import java.util.List;

public interface MachineConnectionRepository {
    /**
     * Get a connection using a machine id
     * @param machineId
     * @return
     */
    public MachineConnection getConnection(String machineId);

    /**
     * Get a connection using an id
     * @param id
     * @return
     */
    public MachineConnection getConnectionById(String id);

    /**
     * Create a new machine connection if not exists
     * @param connection
     */
    public void createConnection(MachineConnection connection);

    /**
     * Get all existing connections
     * @return
     */
    public List<MachineConnection> getConnections();

    /**
     * check if a connection is active
     * @param id
     * @return
     */
    public boolean isConnectionActive(String id);

    /**
     * update a connection
     * @param connection
     */
    public void updateConnection(MachineConnection connection);

    /**
     * set active flag to false on all connected machines
     */
    public void closeAllConnections();

    /**
     * checks if a machine exists using its id
     * @param machineId
     * @return
     */
    public boolean machineExists(String machineId);

    /**
     * Returns a list of pre-auth purchases for a machine
     * @param machineId
     * @return
     */
    public List<Purchase> getMachinePreAuthorizedPurchases(String machineId);

    /**
     * Get a machine available products
     * @param machineId
     * @return
     */
    public List<Product> getMachineAvailableProducts(String machineId);

    /**
     * create a purchase
     * @param purchase
     */
    public void addPurchase(Purchase purchase);
}
