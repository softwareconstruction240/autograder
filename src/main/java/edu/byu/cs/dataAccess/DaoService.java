package edu.byu.cs.dataAccess;

import edu.byu.cs.dataAccess.sql.PhaseConfigurationSqlDao;
import edu.byu.cs.dataAccess.sql.SubmissionSqlDao;
import edu.byu.cs.dataAccess.sql.UserSqlDao;

public class DaoService {
    public static UserDao getUserDao() {
        return new UserSqlDao();
    }

    public static SubmissionDao getSubmissionDao() {
        return new SubmissionSqlDao();
    }

    public static PhaseConfigurationDao getPhaseConfigurationDao() {
        return new PhaseConfigurationSqlDao();
    }
}
