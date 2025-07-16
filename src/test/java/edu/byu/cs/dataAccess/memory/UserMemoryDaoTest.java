package edu.byu.cs.dataAccess.memory;

import edu.byu.cs.dataAccess.base.UserDaoTest;
import edu.byu.cs.dataAccess.daoInterface.UserDao;

public class UserMemoryDaoTest extends UserDaoTest {
    @Override
    protected UserDao newUserDao() {
        return new UserMemoryDao();
    }

    @Override
    protected void clearUsers() {
        dao = newUserDao();
    }
}
