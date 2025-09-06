package edu.byu.cs.dataAccess.sql;

import edu.byu.cs.dataAccess.DataAccessException;
import edu.byu.cs.dataAccess.base.ConfigurationDaoTest;
import edu.byu.cs.dataAccess.daoInterface.ConfigurationDao;
import org.junit.jupiter.api.BeforeAll;

public class ConfigurationSqlDaoTest extends ConfigurationDaoTest {

    @BeforeAll
    static void setupDB() throws DataAccessException{
        SqlDaoTestUtils.prepareSQLDatabase();
    }

    @Override
    protected ConfigurationDao getConfigurationDao() {
        return new ConfigurationSqlDao();
    }

    @Override
    protected void clearConfigurationItems() throws DataAccessException {
        SqlDaoTestUtils.deleteTableWithCascade("configuration");
    }
}
