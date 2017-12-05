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
import java.math.BigInteger;
import java.util.List;

@Stateless
public class MachineConnectionRepositoryImpl implements MachineConnectionRepository {


    @PersistenceContext
    private EntityManager em;

    @Inject
    private QrCodeService qrCodeService;

    private final Logger log = Logger.getLogger(getClass());

    @Override
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

    @Override
    public MachineConnection getConnection(String machineId) {
        try {
            return (MachineConnection) em.createQuery("SELECT mc FROM MachineConnection mc WHERE mc.machineId = :id")
                    .setParameter("id", machineId).getSingleResult();
        } catch (NoResultException ex) {
            return null;
        }
    }

    @Override
    public MachineConnection getConnectionById(String id) {
        try {
            return (MachineConnection) em.createQuery("SELECT mc FROM MachineConnection mc WHERE mc.id = :id")
                    .setParameter("id", id)
                    .getSingleResult();
        } catch (NoResultException ex) {
            return null;
        }
    }


    @Override
    public void createConnection(MachineConnection connection) {
        try {
            em.persist(connection);
        } catch (Exception ex) {
            log.error("Could not persist machine connection", ex);
        }
    }


    @Override
    public void closeAllConnections() {
        em.createQuery("UPDATE MachineConnection mc SET mc.connectionActive = false").executeUpdate();
    }

    @Override
    public List<Purchase> getMachinePreAuthorizedPurchases(String machineId) {
        return em.createQuery("SELECT p FROM Purchase p WHERE p.status = :status AND p.machineId = :machineId")
                .setParameter("status", PurchaseStatus.PRE_AUTH)
                .setParameter("machineId", machineId)
                .getResultList();
    }

    @Override
    public List<Product> getMachineAvailableProducts(String machineId) {
        return em.createQuery("SELECT m.products FROM Machine m WHERE m.id = :machineId")
                .setParameter("machineId", machineId).getResultList();
    }

    @Override
    public void addPurchase(Purchase purchase) {
        em.persist(purchase);
    }

    @Override
    public int removePreAuthorizedPurchases(String machineId) {
        List<Purchase> machinePurchases = getMachinePreAuthorizedPurchases(machineId);
        for (Purchase purchase : machinePurchases) {
            em.remove(purchase);
        }
        /**return em.createNativeQuery("DELETE FROM purchase WHERE status = ? and machine_id = ?")
         .setParameter(1, PurchaseStatus.PRE_AUTH)
         .setParameter(2, machineId)
         .executeUpdate();**/
        return machinePurchases.size();
    }

    @Override
    public void updatePurchaseStatus(String purchaseId, PurchaseStatus cashed) {
        em.createQuery("UPDATE Purchase p SET p.status = :status WHERE p.id = :id")
                .setParameter("status", cashed)
                .setParameter("id", purchaseId)
                .executeUpdate();
    }

    @Override
    public Purchase getPurchase(String purchaseId) {
        return em.find(Purchase.class, purchaseId);
    }

    @Override
    public List<Machine> getMachines() {
        return em.createQuery("SELECT m FROM Machine m")
                .getResultList();
    }

    @Override
    public void updateMachineConnectionStatus(String connectionId, boolean b) {
        em.createQuery("UPDATE MachineConnection mc SET mc.connectionActive = :value")
                .setParameter("value", b)
                .executeUpdate();
    }

    @Override
    public void persistMachineFail(MachineFail fail) {
        em.persist(fail);
    }

    @Override
    public Machine getMachine(String id) {
        try{
            return (Machine)em.createQuery("SELECT m FROM Machine m WHERE m.id = :id")
                    .setParameter("id", id)
                    .getSingleResult();
        }catch(NoResultException ex){
            return null;
        }
    }
}
