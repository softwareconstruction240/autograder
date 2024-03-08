package edu.byu.cs.dataAccess;

import edu.byu.cs.dataAccess.sql.*;

public class DaoService {
    public static UserDao getUserDao() {
        return new UserSqlDao();
    }

    public static SubmissionDao getSubmissionDao() {
        return new SubmissionSqlDao();
    }

    public static QueueDao getQueueDao() {
        return new QueueSqlDao();
    }

    public static RubricConfigDao getRubricConfigDao() {
        return new RubricConfigSqlDao();
    }
}
