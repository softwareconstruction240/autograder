package edu.byu.cs.dataAccess.sql;

import edu.byu.cs.dataAccess.DataAccessException;
import edu.byu.cs.dataAccess.base.RepoUpdateDaoTest;
import edu.byu.cs.dataAccess.daoInterface.RepoUpdateDao;
import edu.byu.cs.dataAccess.daoInterface.UserDao;
import org.junit.jupiter.api.BeforeAll;

public class RepoUpdateSqlDaoTest extends RepoUpdateDaoTest {
    @BeforeAll
    public static void setupDB() throws DataAccessException{
        SqlDaoTestUtils.prepareSQLDatabase();
    }

    @Override
    protected RepoUpdateDao getRepoUpdateDao() {
        return new RepoUpdateSqlDao();
    }

    @Override
    protected UserDao newUserDao() {
        return new UserSqlDao();
    }

    @Override
    protected void clearRepoUpdateItems() throws DataAccessException {
        SqlDaoTestUtils.deleteTableWithCascade("repo_update");
    }
}
