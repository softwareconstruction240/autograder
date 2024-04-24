package edu.byu.cs.dataAccess;

import edu.byu.cs.dataAccess.sql.*;

public class DaoService {

    private static UserDao userDao = new UserSqlDao();
    private static SubmissionDao submissionDao = new SubmissionSqlDao();
    private static QueueDao queueDao = new QueueSqlDao();
    private static RubricConfigDao rubricConfigDao = new RubricConfigSqlDao();
    private static ConfigurationDao configurationDao = new ConfigurationSqlDao();

    public static UserDao getUserDao() {
        return userDao;
    }

    public static SubmissionDao getSubmissionDao() {
        return submissionDao;
    }

    public static QueueDao getQueueDao() {
        return queueDao;
    }

    public static RubricConfigDao getRubricConfigDao() {
        return rubricConfigDao;
    }

    public static ConfigurationDao getConfigurationDao() {
        return configurationDao;
    }

    public static void setUserDao(UserDao userDao) {
        DaoService.userDao = userDao;
    }

    public static void setSubmissionDao(SubmissionDao submissionDao) {
        DaoService.submissionDao = submissionDao;
    }

    public static void setQueueDao(QueueDao queueDao) {
        DaoService.queueDao = queueDao;
    }

    public static void setRubricConfigDao(RubricConfigDao rubricConfigDao) {
        DaoService.rubricConfigDao = rubricConfigDao;
    }

    public static void setConfigurationDao(ConfigurationDao configurationDao) {
        DaoService.configurationDao = configurationDao;
    }
}
