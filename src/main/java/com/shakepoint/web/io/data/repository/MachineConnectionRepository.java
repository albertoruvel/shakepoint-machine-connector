package com.shakepoint.web.io.data.repository;
import com.shakepoint.web.io.data.entity.*;

import java.util.List;

public interface MachineConnectionRepository {

    public int getSlotNumber(String machineId, String productId);

    public String getProductEngineUseTime(String productId);
    /**
     * check if a port is available or not
     * @param port
     * @return
     */
    public boolean isPortAvailable(int port);
    /**
     * Get a connection using a machine id
     * @param machineId
     * @return
     */
    public MachineConnection getConnection(String machineId);

    /**
     * Create a new machine connection if not exists
     * @param connection
     */
    public void createConnection(MachineConnection connection);


    /**
     * set active flag to false on all connected machines
     */
    public void closeAllConnections();

    /**
     * Returns a list of pre-auth purchases for a machine
     * @param machineId
     * @return
     */
    public List<Purchase> getMachinePreAuthorizedPurchases(String machineId);

    /**
     * create a purchase
     * @param purchase
     */
    public void addPurchase(Purchase purchase);

    /**
     * Updates a purchase status using purchase ID
     * @param purchaseId
     * @param cashed
     */
    public void updatePurchaseStatus(String purchaseId, PurchaseStatus cashed);

    public Purchase getPurchase(String purchaseId);

    List<Machine> getMachines();

    void updateMachineConnectionStatus(String connectionId, boolean b);
    public void persistMachineFail(MachineFail fail);
    public Machine getMachine(String id);
    String getTechnicianEmailByMachineId(final String machineId);
    Product getProductById(String id);
    public void updateProductNutritionalDataUrl(String productId, String url);
    public void updatePurchaseQrCodeUrl(String purchaseId, String url);
    public List<String> getAdminsAndTechniciansEmails(String technicianId);
    public List<Purchase> getPreAuthorizedPurchasesForMachine(String machineId);
    public List<MachineProductStatus> getMachineProducts(String machineId);
}
