package edu.byu.cs.dataAccess.sql;

import edu.byu.cs.dataAccess.DataAccessException;
import edu.byu.cs.dataAccess.base.QueueDaoTest;
import edu.byu.cs.dataAccess.daoInterface.QueueDao;
import edu.byu.cs.dataAccess.daoInterface.UserDao;
import org.junit.jupiter.api.BeforeAll;

public class QueueSqlDaoTest extends QueueDaoTest {
    @BeforeAll
    static void setupDB() throws DataAccessException{
        SqlDaoTestUtils.prepareSQLDatabase();
    }

    @Override
    protected QueueDao getQueueDao() {
        return new QueueSqlDao();
    }

    @Override
    protected UserDao newUserDao() {
        return new UserSqlDao();
    }

    @Override
    protected void clearQueueItems() throws DataAccessException {
        SqlDaoTestUtils.deleteTableWithCascade("queue");
    }
}
