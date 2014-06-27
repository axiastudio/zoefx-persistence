package com.axiastudio.zoefx.persistence;

import com.axiastudio.zoefx.core.db.Database;
import com.axiastudio.zoefx.core.db.Manager;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.util.Map;

/**
 * User: tiziano
 * Date: 18/03/14
 * Time: 20:46
 */
public class JPADatabaseImpl implements Database {

    private EntityManagerFactory entityManagerFactory;

    public EntityManagerFactory getEntityManagerFactory() {
        return entityManagerFactory;
    }

    /**
     * Initialize the entity manager factory.
     *
     * @param persistenceUnit The persistence unit defined in persistence.xml
     *
     */
    @Override
    public void open(String persistenceUnit) {
        this.entityManagerFactory = Persistence.createEntityManagerFactory(persistenceUnit);
    }
    /**
     * Initialize the entity manager factory.
     *
     * @param persistenceUnit The persistence unit defined in persistence.xml
     * @param properties Properties in overriding
     *
     */
    @Override
    public void open(String persistenceUnit, Map<String, String> properties) {
        this.entityManagerFactory = Persistence.createEntityManagerFactory(persistenceUnit, properties);
    }

    /**
     * Create a controller for the entities of the given class.
     *
     * @param klass The class managed from the controller
     *
     */
    @Override
    public <E> Manager<E> createManager(Class<E> klass){
        JPAManagerImpl<E> manager = new JPAManagerImpl(getEntityManagerFactory().createEntityManager(), klass);
        return manager;
    }
}