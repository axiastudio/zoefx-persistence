package com.axiastudio.zoefx.persistence;

import com.axiastudio.zoefx.core.Utilities;
import com.axiastudio.zoefx.core.db.Database;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * User: tiziano
 * Date: 18/03/14
 * Time: 21:25
 */
public class JPADatabaseImplTest {

    @BeforeClass
    public static void setUpClass() throws Exception {
        JPADatabaseImpl db = new JPADatabaseImpl();
        Utilities.registerUtility(db, Database.class);
    }

    @Test
    public void testRegisterUtility() throws Exception {
        Database db = Utilities.queryUtility(Database.class);
    }

}
