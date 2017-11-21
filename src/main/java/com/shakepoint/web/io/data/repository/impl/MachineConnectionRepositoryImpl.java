package com.shakepoint.web.io.data.repository.impl;

import com.shakepoint.web.io.data.entity.MachineConnection;
import com.shakepoint.web.io.data.repository.MachineConnectionRepository;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import java.util.List;

@Stateless
public class MachineConnectionRepositoryImpl implements MachineConnectionRepository {


    @PersistenceContext
    private EntityManager em;

    @Override
    public MachineConnection getConnection(String machineId) {
        try{
            return (MachineConnection)em.createQuery("SELECT mc FROM MachineConnection mc WHERE mc.machineId = :id")
                    .setParameter("id", machineId).getSingleResult();
        }catch(NoResultException ex){
            return null;
        }
    }

    @Override
    public MachineConnection getConnectionById(String id) {
        try{
            return (MachineConnection)em.createQuery("SELECT mc FROM MachineConnection mc WHERE mc.id = :d")
                    .setParameter("id", id)
                    .getSingleResult();
        }catch(NoResultException ex){
            return null;
        }
    }


    @Override
    public void createConnection(MachineConnection connection) {
        try{
            em.persist(connection);
        }catch(Exception ex){

        }
    }

    @Override
    public List<MachineConnection> getConnections() {
        return em.createQuery("SELECT mc FROM MachineConnection mc")
                .getResultList();
    }

    @Override
    public boolean isConnectionActive(String id) {
        return ((Boolean)em.createQuery("SELECT mc.connectionActive FROM MachineConnection mc WHERE mc.id = :id")
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
}
