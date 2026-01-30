package edu.byu.cs.dataAccess.memory;

import edu.byu.cs.dataAccess.DataAccessException;
import edu.byu.cs.dataAccess.base.RepoUpdateDaoTest;
import edu.byu.cs.dataAccess.daoInterface.RepoUpdateDao;
import edu.byu.cs.dataAccess.daoInterface.UserDao;

public class RepoUpdateMemoryDaoTest extends RepoUpdateDaoTest {
    @Override
    protected RepoUpdateDao getRepoUpdateDao() {
        return new RepoUpdateMemoryDao();
    }

    @Override
    protected UserDao newUserDao() {
        return new UserMemoryDao();
    }

    @Override
    protected void clearRepoUpdateItems() {
        dao = new RepoUpdateMemoryDao();
    }
}
