package edu.byu.cs.dataAccess.memory;

import edu.byu.cs.dataAccess.base.QueueDaoTest;
import edu.byu.cs.dataAccess.daoInterface.QueueDao;
import edu.byu.cs.dataAccess.daoInterface.UserDao;

public class QueueMemoryDaoTest extends QueueDaoTest {
    @Override
    protected QueueDao getQueueDao() {
        return new QueueMemoryDao();
    }

    @Override
    protected UserDao newUserDao() {
        return new UserMemoryDao();
    }

    @Override
    protected void clearQueueItems() {
        dao = new QueueMemoryDao();
    }
}
