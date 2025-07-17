package edu.byu.cs.dataAccess.memory;

import edu.byu.cs.dataAccess.base.QueueDaoTest;
import edu.byu.cs.dataAccess.daoInterface.QueueDao;

public class QueueMemoryDaoTest extends QueueDaoTest {
    @Override
    protected QueueDao getQueueDao() {
        return new QueueMemoryDao();
    }

    @Override
    protected void clearQueueItems() {
        dao = new QueueMemoryDao();
    }
}
