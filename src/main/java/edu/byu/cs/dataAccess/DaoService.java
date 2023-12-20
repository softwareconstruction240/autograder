package edu.byu.cs.dataAccess;

import edu.byu.cs.dataAccess.memory.SubmissionMemoryDao;
import edu.byu.cs.dataAccess.memory.UserMemoryDao;

public class DaoService {
    public static UserDao getUserDao() {
        return new UserMemoryDao();
    }

    public static SubmissionDao getSubmissionDao() {
        return new SubmissionMemoryDao();
    }
}
