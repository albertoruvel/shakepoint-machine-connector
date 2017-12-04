package com.shakepoint.web.io.data.repository.impl;

import com.shakepoint.web.io.data.entity.MachineConnection;
import com.shakepoint.web.io.data.entity.Product;
import com.shakepoint.web.io.data.entity.Purchase;
import com.shakepoint.web.io.data.entity.PurchaseStatus;
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
    public List<MachineConnection> getConnections() {
        return em.createQuery("SELECT mc FROM MachineConnection mc")
                .getResultList();
    }

    @Override
    public boolean isConnectionActive(String id) {
        return ((Boolean) em.createQuery("SELECT mc.connectionActive FROM MachineConnection mc WHERE mc.id = :id")
                .setParameter("id", id).getSingleResult());
    }

    @Override
    public void updateConnection(MachineConnection connection) {
        em.merge(connection);
    }

    @Override
    public void closeAllConnections() {
        em.createQuery("UPDATE mc FROM MachineConnection mc SET mc.connectionActive = false").executeUpdate();
    }

    @Override
    public boolean machineExists(String machineId) {
        return ((BigInteger) em.createNativeQuery("SELECT COUNT(*) FROM machine WHERE id = ?")
                .setParameter(1, machineId).getSingleResult()).intValue() > 0;
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
        for (Purchase purchase : machinePurchases){
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
        em.createQuery("UPDATE p FROM Purchase p SET p.status = :status WHERE p.id = :id")
                .setParameter("status", cashed)
                .setParameter("id", purchaseId)
                .executeUpdate();
    }

    @Override
    public Purchase getPurchase(String purchaseId) {
        return em.find(Purchase.class, purchaseId);
    }
}
