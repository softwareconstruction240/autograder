package edu.byu.cs.dataAccess.memory;

import edu.byu.cs.dataAccess.DataAccessException;
import edu.byu.cs.dataAccess.base.ConfigurationDaoTest;
import edu.byu.cs.dataAccess.daoInterface.ConfigurationDao;

public class ConfigurationMemoryDaoTest extends ConfigurationDaoTest {
    @Override
    protected ConfigurationDao getConfigurationDao() {
        return new ConfigurationMemoryDao();
    }

    @Override
    protected void clearConfigurationItems() {
        dao = new ConfigurationMemoryDao();
    }
}
