package edu.byu.cs.dataAccess.sql;

import edu.byu.cs.dataAccess.DataAccessException;
import edu.byu.cs.dataAccess.base.RubricConfigDaoTest;
import edu.byu.cs.dataAccess.daoInterface.RubricConfigDao;
import org.junit.jupiter.api.BeforeAll;

public class RubricConfigSqlDaoTest extends RubricConfigDaoTest {
    @BeforeAll
    static void setupDB() throws DataAccessException {
        SqlDaoTestUtils.prepareSQLDatabase();
    }

    @Override
    protected RubricConfigDao getRubricConfigDao() {
        return new RubricConfigSqlDao();
    }

    @Override
    protected void clearRubricConfigDao() throws DataAccessException{
        SqlDaoTestUtils.deleteTableWithCascade("rubric_config");
    }
}
