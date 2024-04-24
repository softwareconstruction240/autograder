package edu.byu.cs.dataAccess;

import edu.byu.cs.dataAccess.memory.*;

public class DaoService {

    private static UserDao userDao = new UserMemoryDao();
    private static SubmissionDao submissionDao = new SubmissionMemoryDao();
    private static QueueDao queueDao = new QueueMemoryDao();
    private static RubricConfigDao rubricConfigDao = new RubricConfigMemoryDao();
    private static ConfigurationDao configurationDao = new ConfigurationMemoryDao();

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
