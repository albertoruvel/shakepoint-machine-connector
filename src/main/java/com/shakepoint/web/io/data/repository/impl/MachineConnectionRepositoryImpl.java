package com.shakepoint.web.io.data.repository.impl;

import com.shakepoint.web.io.data.dto.req.socket.QRCodeValidationMachineMessage;
import com.shakepoint.web.io.data.entity.MachineConnection;
import com.shakepoint.web.io.data.entity.Product;
import com.shakepoint.web.io.data.entity.Purchase;
import com.shakepoint.web.io.data.entity.PurchaseStatus;
import com.shakepoint.web.io.data.repository.MachineConnectionRepository;
import com.shakepoint.web.io.service.QrCodeService;

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
        em.createQuery("UPDATE mc FROM MachineConnection SET mc.connectionActive = false").executeUpdate();
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
}
