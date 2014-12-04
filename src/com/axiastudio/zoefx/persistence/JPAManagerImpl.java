/*
 * Copyright (c) 2014, AXIA Studio (Tiziano Lattisi) - http://www.axiastudio.com
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the AXIA Studio nor the
 *    names of its contributors may be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY AXIA STUDIO ''AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL AXIA STUDIO BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.axiastudio.zoefx.persistence;

import com.axiastudio.zoefx.core.Utilities;
import com.axiastudio.zoefx.core.model.beans.BeanAccess;
import com.axiastudio.zoefx.core.model.beans.BeanClassAccess;
import com.axiastudio.zoefx.core.db.Database;
import com.axiastudio.zoefx.core.db.Manager;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

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
            return entityClass.newInstance();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public Object createRow(String collectionName) {
        Database db = Utilities.queryUtility(Database.class);
        BeanClassAccess beanClassAccess = new BeanClassAccess(entityClass, collectionName);
        Class<?> genericReturnType = beanClassAccess.getGenericReturnType();
        Manager<?> manager = db.createManager(genericReturnType);
        return manager.create();
    }

    @Override
    public E save(E entity) {
        parentize(entity);
        EntityManager em = getEntityManager();
        em.getTransaction().begin();
        E merged = em.merge(entity);
        em.getTransaction().commit();
        return merged;
    }

    @Override
    public void save(List<E> entities) {
        EntityManager em = getEntityManager();
        em.getTransaction().begin();
        for( E entity: entities ){
            em.merge(entity);
        }
        em.getTransaction().commit();
    }

    @Override
    public void delete(E entity) {
        EntityManager em = getEntityManager();
        em.getTransaction().begin();
        E merged = em.merge(entity);
        em.remove(merged);
        em.getTransaction().commit();
    }

    @Override
    public void deleteRow(Object row) {
        EntityManager em = getEntityManager();
        Object merged = em.merge(row);
        em.remove(merged);
    }

    @Override
    public void truncate(){
        EntityManager em = getEntityManager();
        em.getTransaction().begin();
        em.createQuery("DELETE FROM " + entityClass.getCanonicalName() + " e").executeUpdate();
        em.getTransaction().commit();
    }

    @Override
    public E get(Long id) {
        EntityManager em = getEntityManager();
        return em.find(entityClass, id);
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

    @Override
    public List<E> query(Map<String, Object> map) {
        EntityManager em = getEntityManager();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<E> cq = cb.createQuery(entityClass);
        Root<E> root = cq.from(entityClass);
        List<Predicate> predicates = new ArrayList<>();
        for( String name: map.keySet() ){
            Predicate predicate=null;
            Path path = null;
            path = root.get(name);
            Object objectValue = map.get(name);
            if( objectValue instanceof String ){
                String value = (String) objectValue;
                value = value.replace("*", "%");
                if( !value.endsWith("%") ){
                    value += "%";
                }
                predicate = cb.like(cb.upper(path), value.toUpperCase());
            } else if( objectValue instanceof Boolean ){
                predicate = cb.equal(path, objectValue);
            } else if( objectValue instanceof List ){
                List<Date> range = (List<Date>) objectValue;
                Date from = zeroMilliseconds(range.get(0));
                Date to = lastMillisecond(range.get(1));
                predicate = cb.and(cb.greaterThanOrEqualTo(path, from),
                        cb.lessThanOrEqualTo(path, to));
            } else if( objectValue instanceof Object ){
                if( objectValue.getClass().isEnum() ) {
                    int value = ((Enum) objectValue).ordinal(); // XXX: and if EnumType.STRING??
                    predicate = cb.equal(path, value);
                } else {
                    predicate = cb.equal(path, objectValue);
                }
            }
            if( predicate != null ){
                predicates.add(predicate);
            }
        }

        cq.select(root);
        if( predicates.size()>0 ){
            cq.where(cb.and(predicates.toArray(new Predicate[predicates.size()])));
        }
        TypedQuery<E> query = em.createQuery(cq);
        List<E> store = query.getResultList();
        return store;
    }

    private Date zeroMilliseconds(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    private Date lastMillisecond(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        return calendar.getTime();
    }

    protected EntityManager getEntityManager() {
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
