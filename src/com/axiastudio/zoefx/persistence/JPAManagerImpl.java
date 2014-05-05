package com.axiastudio.zoefx.persistence;

import com.axiastudio.zoefx.core.beans.BeanAccess;
import com.axiastudio.zoefx.core.db.Manager;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * User: tiziano
 * Date: 18/03/14
 * Time: 20:50
 */
public class JPAManagerImpl<E> implements Manager<E> {

    private Class<E> entityClass;
    private EntityManager entityManager;

    public JPAManagerImpl(EntityManager em, Class<E> klass) {
        entityClass = klass;
        entityManager = em;
    }

    @Override
    public E create() {
        try {
            E entity = entityClass.newInstance();
            return entity;
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public E commit(E entity) {
        parentize(entity);
        EntityManager em = getEntityManager();
        em.getTransaction().begin();
        E merged = em.merge(entity);
        em.getTransaction().commit();
        return merged;
    }

    @Override
    public void delete(E entity) {

    }

    @Override
    public void truncate(){
        EntityManager em = getEntityManager();
        em.getTransaction().begin();
        em.createQuery("DELETE FROM " + entityClass.getCanonicalName() + " e").executeUpdate();
        em.getTransaction().commit();
        em.close();
    }

    @Override
    public E get(Long id) {
        return null;
    }

    @Override
    public List<E> getAll() {
        EntityManager em = getEntityManager();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<E> cq = cb.createQuery(entityClass);
        Root<E> root = cq.from(entityClass);
        cq.select(root);
        TypedQuery<E> query = em.createQuery(cq);
        List<E> store = query.getResultList();
        return store;
    }

    private EntityManager getEntityManager() {
        return entityManager;
    }

    /*
     * The parentize method hooks the items of the collections to the parent
     * entity.
     */
    private void parentize(E entity){
        for(Field f: entityClass.getDeclaredFields()){
            for( Annotation a: f.getAnnotations()){
                // discover the OneToMany
                if( a.annotationType().equals(javax.persistence.OneToMany.class) ) {
                    String name = f.getName();
                    BeanAccess<Collection> collectionBeanAccess = new BeanAccess<Collection>(entity, name);
                    Collection collection = collectionBeanAccess.getValue();
                    if( collection != null && collection.size()>0 ){
                        // discover the "mapped by" foreign key
                        String foreignKey=null;
                        try {
                            Method mappedBy = a.annotationType().getDeclaredMethod("mappedBy");
                            foreignKey = (String) mappedBy.invoke(a);
                        } catch (NoSuchMethodException e) {
                            e.printStackTrace();
                        } catch (InvocationTargetException e) {
                            e.printStackTrace();
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        }
                        if( foreignKey != null ) {
                            // parentize children
                            for (Iterator it = collection.iterator(); it.hasNext(); ) {
                                Object child = it.next();
                                BeanAccess<E> fkBeanAccess = new BeanAccess<>(child, foreignKey);
                                fkBeanAccess.setValue(entity);
                            }
                        }
                    }
                }
            }
        }
    }

}
