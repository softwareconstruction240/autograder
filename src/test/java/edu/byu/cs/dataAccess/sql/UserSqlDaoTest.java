package edu.byu.cs.dataAccess.sql;

import edu.byu.cs.dataAccess.DataAccessException;
import edu.byu.cs.dataAccess.base.UserDaoTest;
import edu.byu.cs.dataAccess.daoInterface.UserDao;
import org.junit.jupiter.api.BeforeAll;

public class UserSqlDaoTest extends UserDaoTest {
    @BeforeAll
    protected static void prepareDatabase() throws DataAccessException {
        SqlDaoTestUtils.prepareSQLDatabase();
    }

    @Override
    protected UserDao newUserDao() {
        return new UserSqlDao();
    }

    @Override
    protected void clearUsers() throws DataAccessException{
        SqlDaoTestUtils.deleteTableWithCascade("user");
    }
}
