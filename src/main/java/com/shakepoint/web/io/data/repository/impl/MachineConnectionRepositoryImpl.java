package com.shakepoint.web.io.data.repository.impl;

import com.shakepoint.web.io.data.entity.*;
import com.shakepoint.web.io.data.repository.MachineConnectionRepository;
import com.shakepoint.web.io.service.QrCodeService;
import org.apache.log4j.Logger;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.math.BigInteger;
import java.util.List;

@Stateless
public class MachineConnectionRepositoryImpl implements MachineConnectionRepository {


    @PersistenceContext
    private EntityManager em;

    @Inject
    private QrCodeService qrCodeService;

    private final Logger log = Logger.getLogger(getClass());

    public int getSlotNumber(String machineId, String productId) {
        return (Integer)em.createNativeQuery("SELECT slot_number FROM machine_product WHERE machine_id = ? AND product_id = ?")
                .setParameter(1, machineId)
                .setParameter(2, productId)
                .getSingleResult();
    }

    public String getProductEngineUseTime(String productId) {
        Integer use =  (Integer)em.createQuery("SELECT p.engineUseTime FROM Product p WHERE p.id = :id")
                .setParameter("id", productId)
                .getSingleResult();
        return use.toString();
    }

    public boolean isPortAvailable(int port) {
        try {
            Long p = (Long) em.createQuery("SELECT COUNT(mc.port) FROM MachineConnection mc WHERE mc.port = :port")
                    .setParameter("port", port)
                    .getSingleResult();
            return p.intValue() == 0;
        } catch (Exception ex) {
            return false;
        }
    }

    public MachineConnection getConnection(String machineId) {
        try {
            return (MachineConnection) em.createQuery("SELECT mc FROM MachineConnection mc WHERE mc.machineId = :id")
                    .setParameter("id", machineId).getSingleResult();
        } catch (NoResultException ex) {
            return null;
        }
    }

    public MachineConnection getConnectionById(String id) {
        try {
            return (MachineConnection) em.createQuery("SELECT mc FROM MachineConnection mc WHERE mc.id = :id")
                    .setParameter("id", id)
                    .getSingleResult();
        } catch (NoResultException ex) {
            return null;
        }
    }


    public void createConnection(MachineConnection connection) {
        try {
            em.persist(connection);
        } catch (Exception ex) {
            log.error("Could not persist machine connection", ex);
        }
    }


    public void closeAllConnections() {
        em.createQuery("UPDATE MachineConnection mc SET mc.connectionActive = false").executeUpdate();
    }

    public List<Purchase> getMachinePreAuthorizedPurchases(String machineId) {
        return em.createQuery("SELECT p FROM Purchase p WHERE p.status = :status AND p.machineId = :machineId")
                .setParameter("status", PurchaseStatus.PRE_AUTH)
                .setParameter("machineId", machineId)
                .getResultList();
    }

    public List<Product> getMachineAvailableProducts(String machineId) {
        return em.createQuery("SELECT m.products FROM Machine m WHERE m.id = :machineId")
                .setParameter("machineId", machineId).getResultList();
    }

    public void addPurchase(Purchase purchase) {
        em.persist(purchase);
    }

    public int removePreAuthorizedPurchases(String machineId) {
        List<Purchase> machinePurchases = getMachinePreAuthorizedPurchases(machineId);
        for (Purchase purchase : machinePurchases) {
            purchase.setStatus(PurchaseStatus.CANCELLED);
            em.merge(purchase);
        }
        return machinePurchases.size();
    }

    public void updatePurchaseStatus(String purchaseId, PurchaseStatus cashed) {
        em.createQuery("UPDATE Purchase p SET p.status = :status WHERE p.id = :id")
                .setParameter("status", cashed)
                .setParameter("id", purchaseId)
                .executeUpdate();
    }

    public Purchase getPurchase(String purchaseId) {
        return em.find(Purchase.class, purchaseId);
    }

    public List<Machine> getMachines() {
        return em.createQuery("SELECT m FROM Machine m")
                .getResultList();
    }

    public void updateMachineConnectionStatus(String connectionId, boolean b) {
        em.createQuery("UPDATE MachineConnection mc SET mc.connectionActive = :value")
                .setParameter("value", b)
                .executeUpdate();
    }

    public void persistMachineFail(MachineFail fail) {
        em.persist(fail);
    }

    public Machine getMachine(String id) {
        try{
            return (Machine)em.createQuery("SELECT m FROM Machine m WHERE m.id = :id")
                    .setParameter("id", id)
                    .getSingleResult();
        }catch(NoResultException ex){
            return null;
        }
    }

    public String getTechnicianEmailByMachineId(final String machineId){
        Query query = em.createNativeQuery("select distinct u.email from machine m inner join user u on u.id = m.technician_id  where m.id = :machineId");
        query.setParameter("machineId", machineId);
        return (String)query.getSingleResult();
    }

    public Product getProductById(String id){
        return em.find(Product.class, id);
    }
}
