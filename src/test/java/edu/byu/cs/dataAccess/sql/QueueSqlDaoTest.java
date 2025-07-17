package edu.byu.cs.dataAccess.sql;

import edu.byu.cs.dataAccess.DataAccessException;
import edu.byu.cs.dataAccess.base.QueueDaoTest;
import edu.byu.cs.dataAccess.daoInterface.QueueDao;
import org.junit.jupiter.api.BeforeAll;

public class QueueSqlDaoTest extends QueueDaoTest {
    @BeforeAll
    static void setup() throws DataAccessException{
        SqlDaoTestUtils.prepareSQLDatabase();
    }

    @Override
    protected QueueDao getQueueDao() {
        return new QueueSqlDao();
    }

    @Override
    protected void clearQueueItems() throws DataAccessException {
        SqlDaoTestUtils.deleteTableWithCascade("queue");
    }
}
