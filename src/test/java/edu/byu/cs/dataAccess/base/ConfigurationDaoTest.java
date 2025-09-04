package edu.byu.cs.dataAccess.base;

import edu.byu.cs.dataAccess.DataAccessException;
import edu.byu.cs.dataAccess.daoInterface.ConfigurationDao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public abstract class ConfigurationDaoTest {
    protected ConfigurationDao dao;
    protected abstract ConfigurationDao getConfigurationDao();
    protected abstract void clearConfigurationItems() throws DataAccessException;

    @BeforeEach
    void setup() throws DataAccessException {
        dao = getConfigurationDao();
        clearConfigurationItems();
    }

    @Test
    void getAndSetValidConfigurations(){

    }

    @Test
    void setItemWithInvalidKey(){

    }

    @Test
    void setItemWithInvalidValue(){

    }

    @Test
    void setItemWithDuplicateKey(){

    }

    @Test
    void getItemThatDoesNotExist(){

    }
}
