package edu.byu.cs.dataAccess.memory;

import edu.byu.cs.dataAccess.DataAccessException;
import edu.byu.cs.dataAccess.base.RepoUpdateDaoTest;
import edu.byu.cs.dataAccess.daoInterface.RepoUpdateDao;

public class RepoUpdateMemoryDaoTest extends RepoUpdateDaoTest {
    @Override
    protected RepoUpdateDao getRepoUpdateDao() {
        return new RepoUpdateMemoryDao();
    }

    @Override
    protected void clearRepoUpdateItems() {
        dao = new RepoUpdateMemoryDao();
    }
}
