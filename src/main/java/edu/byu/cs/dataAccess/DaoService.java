package edu.byu.cs.dataAccess;

import edu.byu.cs.dataAccess.daoInterface.*;
import edu.byu.cs.dataAccess.memory.*;
import edu.byu.cs.dataAccess.sql.*;

/**
 * Provides centralized access and management for all DAOs.
 * <br>
 * This class maintains static references to each DAO implementation, enabling for retrieval
 * and configuration of DAO instances. The {@code initializeMemoryDAOs()} method configures
 * in-memory DAO instances with default testing values, while {@code initializeSqlDAOs()}
 * initializes database-backed DAOs for live use.
 */
public class DaoService {

    private static UserDao userDao = new UserMemoryDao();
    private static SubmissionDao submissionDao = new SubmissionMemoryDao();
    private static QueueDao queueDao = new QueueMemoryDao();
    private static RubricConfigDao rubricConfigDao = new RubricConfigMemoryDao();
    private static ConfigurationDao configurationDao = new ConfigurationMemoryDao();
    private static RepoUpdateDao repoUpdateDao = new RepoUpdateMemoryDao();

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

    public static void setRepoUpdateDao(RepoUpdateDao repoUpdateDao) {
        DaoService.repoUpdateDao = repoUpdateDao;
    }

    public static RepoUpdateDao getRepoUpdateDao() { return repoUpdateDao; }

    /** Create and set a memory DAO for every DAO. Used for testing purposes. */
    public static void initializeMemoryDAOs() {
        DaoService.setRubricConfigDao(new RubricConfigMemoryDao());
        DaoService.setUserDao(new UserMemoryDao());
        DaoService.setQueueDao(new QueueMemoryDao());
        DaoService.setSubmissionDao(new SubmissionMemoryDao());
        DaoService.setConfigurationDao(new ConfigurationMemoryDao());
        DaoService.setRepoUpdateDao(new RepoUpdateMemoryDao());

        /* Initialize crucial default values in Config for testing purposes */
        try {
            configurationDao.setConfiguration(ConfigurationDao.Configuration.GIT_COMMIT_PENALTY, 0.1f, Float.class);
            configurationDao.setConfiguration(ConfigurationDao.Configuration.MAX_LATE_DAYS_TO_PENALIZE, 5, Integer.class);
            configurationDao.setConfiguration(ConfigurationDao.Configuration.PER_DAY_LATE_PENALTY, 0.1f, Float.class);
            configurationDao.setConfiguration(ConfigurationDao.Configuration.LINES_PER_COMMIT_REQUIRED, 5, Integer.class);
            configurationDao.setConfiguration(ConfigurationDao.Configuration.CLOCK_FORGIVENESS_MINUTES, 3, Integer.class);
        } catch (DataAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /** Create and set a SQL DAO for every DAO. Used for live application purposes */
    public static void initializeSqlDAOs() throws DataAccessException {
        SqlDb.setUpDb();
        DaoService.setConfigurationDao(new ConfigurationSqlDao());
        DaoService.setQueueDao(new QueueSqlDao());
        DaoService.setRubricConfigDao(new RubricConfigSqlDao());
        DaoService.setSubmissionDao(new SubmissionSqlDao());
        DaoService.setUserDao(new UserSqlDao());
        DaoService.setRepoUpdateDao(new RepoUpdateSqlDao());
    }
}
