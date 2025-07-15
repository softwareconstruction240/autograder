package edu.byu.cs.dataAccess.memory;

import edu.byu.cs.dataAccess.DataAccessException;
import edu.byu.cs.dataAccess.base.SubmissionDaoTest;
import edu.byu.cs.dataAccess.daoInterface.SubmissionDao;
import edu.byu.cs.dataAccess.daoInterface.UserDao;

public class SubmissionMemoryDaoTest extends SubmissionDaoTest {
    @Override
    protected SubmissionDao newSubmissionDao() {
        return new SubmissionMemoryDao();
    }

    @Override
    protected UserDao newUserDao() {
        return new UserMemoryDao();
    }

    @Override
    protected void clearSubmissions() throws DataAccessException {
        dao = newSubmissionDao();
    }
}
