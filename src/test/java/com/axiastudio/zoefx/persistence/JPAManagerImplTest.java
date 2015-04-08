/*
 * Copyright (c) 2015, AXIA Studio (Tiziano Lattisi) - http://www.axiastudio.com
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

import com.axiastudio.zoefx.core.IOC;
import com.axiastudio.zoefx.core.db.Database;
import com.axiastudio.zoefx.core.db.Manager;
import com.axiastudio.zoefx.core.beans.EntityBuilder;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;

public class JPAManagerImplTest {

    @BeforeClass
    public static void setUpClass() throws Exception {
        JPADatabaseImpl db = new JPADatabaseImpl();
        db.open("testPU");
        IOC.registerUtility(db, Database.class);
        Manager<Author> manager = db.createManager(Author.class);
        Author lev = EntityBuilder.create(Author.class).set("id", 1L).set("name", "Lev").set("surname", "Tolstoj").build();
        manager.save(lev);
        Author hesse = EntityBuilder.create(Author.class).set("id", 2L).set("name", "Hermann").set("surname", "Hesse").build();
        manager.save(hesse);
        Author gabriel = EntityBuilder.create(Author.class).set("id", 3L).set("name", "Gabriel García").set("surname", "Márquez").build();
        manager.save(gabriel);
    }

    @After
    public void tearDown() throws Exception {
        Database db = IOC.queryUtility(Database.class);
        Manager<Author> manager = db.createManager(Author.class);
    }

    @Test
    public void testLimit() throws Exception {
        Database db = IOC.queryUtility(Database.class);
        Manager<Author> manager = db.createManager(Author.class);
        assert manager.query().size() == 3;
        assert manager.query(2).size() == 2;
    }

    @Test
    public void testOderBy() throws Exception {
        Database db = IOC.queryUtility(Database.class);
        Manager<Author> manager = db.createManager(Author.class);
        assert "Gabriel García".equals(manager.query("name").get(0).name);
        assert "Tolstoj".equals(manager.query("surname", Boolean.TRUE, 1).get(0).surname);
    }

    @Test
    public void testPaging() throws Exception {
        Database db = IOC.queryUtility(Database.class);
        Manager<Author> manager = db.createManager(Author.class);
        List<Author> rs = manager.query("id", 2, 2); // second page (page size is 2)
        /*
         *  1st page: Tolstoj, Hesse
         *  2nd page: Márquez
         */
        assert rs.size()==1;
        assert "Márquez".equals(rs.get(0).surname);
    }

}