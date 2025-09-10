package edu.byu.cs.dataAccess.base;

import edu.byu.cs.dataAccess.DataAccessException;
import edu.byu.cs.dataAccess.daoInterface.RepoUpdateDao;
import org.junit.jupiter.api.BeforeEach;

public abstract class RepoUpdateDaoTest {
    protected RepoUpdateDao dao;
    protected abstract RepoUpdateDao getRepoUpdateDao();
    protected abstract void clearRepoUpdateItems() throws DataAccessException;

    @BeforeEach
    public void setup() throws DataAccessException{
        dao = getRepoUpdateDao();
        clearRepoUpdateItems();
    }
}
