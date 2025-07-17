package edu.byu.cs.dataAccess.base;

import edu.byu.cs.dataAccess.DataAccessException;
import edu.byu.cs.dataAccess.daoInterface.QueueDao;
import org.junit.jupiter.api.BeforeEach;

public abstract class QueueDaoTest {
    protected QueueDao dao;
    protected abstract QueueDao getQueueDao();
    protected abstract void clearQueueItems() throws DataAccessException;

    @BeforeEach
    void setup() throws DataAccessException{
        dao = getQueueDao();
        clearQueueItems();
    }
}
