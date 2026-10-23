package edu.byu.cs.dataAccess;

import java.util.Objects;

import edu.byu.cs.dataAccess.daoInterface.ConfigurationDao;
import edu.byu.cs.dataAccess.daoInterface.QueueDao;
import edu.byu.cs.dataAccess.daoInterface.RepoUpdateDao;
import edu.byu.cs.dataAccess.daoInterface.RubricConfigDao;
import edu.byu.cs.dataAccess.daoInterface.SubmissionDao;
import edu.byu.cs.dataAccess.daoInterface.UserDao;
import edu.byu.cs.dataAccess.memory.ConfigurationMemoryDao;
import edu.byu.cs.dataAccess.memory.QueueMemoryDao;
import edu.byu.cs.dataAccess.memory.RepoUpdateMemoryDao;
import edu.byu.cs.dataAccess.memory.RubricConfigMemoryDao;
import edu.byu.cs.dataAccess.memory.SubmissionMemoryDao;
import edu.byu.cs.dataAccess.memory.UserMemoryDao;
import edu.byu.cs.dataAccess.sql.ConfigurationSqlDao;
import edu.byu.cs.dataAccess.sql.QueueSqlDao;
import edu.byu.cs.dataAccess.sql.RepoUpdateSqlDao;
import edu.byu.cs.dataAccess.sql.RubricConfigSqlDao;
import edu.byu.cs.dataAccess.sql.SqlDb;
import edu.byu.cs.dataAccess.sql.SubmissionSqlDao;
import edu.byu.cs.dataAccess.sql.UserSqlDao;

/**
 * Provides centralized access and management for all DAOs.
 * <br>
 * This class stores references to each DAO implementation, enabling for retrieval,
 * with static methods to enable configuration of DAO instances. The {@code initializeMemoryDAOs()}
 * method configures in-memory DAO instances with default testing values, while {@code initializeSqlDAOs()}
 * initializes database-backed DAOs for live use.
 */
public class DaoService {

    private final UserDao userDao;
    private final SubmissionDao submissionDao;
    private final QueueDao queueDao;
    private final RubricConfigDao rubricConfigDao;
    private final ConfigurationDao configurationDao;
    private final RepoUpdateDao repoUpdateDao;

    public DaoService(
            UserDao userDao,
            SubmissionDao submissionDao,
            QueueDao queueDao,
            RubricConfigDao rubricConfigDao,
            ConfigurationDao configurationDao,
            RepoUpdateDao repoUpdateDao) {
        this.userDao = Objects.requireNonNull(userDao);
        this.submissionDao = Objects.requireNonNull(submissionDao);
        this.queueDao = Objects.requireNonNull(queueDao);
        this.rubricConfigDao = Objects.requireNonNull(rubricConfigDao);
        this.configurationDao = Objects.requireNonNull(configurationDao);
        this.repoUpdateDao = Objects.requireNonNull(repoUpdateDao);
    }

    public UserDao getUserDao() { return userDao; }
    public SubmissionDao getSubmissionDao() { return submissionDao; }
    public QueueDao getQueueDao() { return queueDao; }
    public RubricConfigDao getRubricConfigDao() { return rubricConfigDao; }
    public ConfigurationDao getConfigurationDao() { return configurationDao; }
    public RepoUpdateDao getRepoUpdateDao() { return repoUpdateDao; }

    /** Creates isolated in-memory DAO implementations for tests and local use. */
    public static DaoService createMemoryDAOs() {
        ConfigurationDao configurationDao = new ConfigurationMemoryDao();
        try {
            configurationDao.setConfiguration(ConfigurationDao.Configuration.GIT_COMMIT_PENALTY, 0.1f, Float.class);
            configurationDao.setConfiguration(ConfigurationDao.Configuration.MAX_LATE_DAYS_TO_PENALIZE, 5, Integer.class);
            configurationDao.setConfiguration(ConfigurationDao.Configuration.PER_DAY_LATE_PENALTY, 0.1f, Float.class);
            configurationDao.setConfiguration(ConfigurationDao.Configuration.LINES_PER_COMMIT_REQUIRED, 5, Integer.class);
            configurationDao.setConfiguration(ConfigurationDao.Configuration.CLOCK_FORGIVENESS_MINUTES, 3, Integer.class);
            configurationDao.setConfiguration(ConfigurationDao.Configuration.COVERAGE_PERCENT, 0.8f, Float.class);
            configurationDao.setConfiguration(ConfigurationDao.Configuration.EXTRA_COVERAGE_PERCENT, 0.9f, Float.class);
            configurationDao.setConfiguration(ConfigurationDao.Configuration.COVERAGE_TYPE, "LINE", String.class);
        } catch (DataAccessException e) {
            throw new RuntimeException(e);
        }
        return new DaoService(
                new UserMemoryDao(), new SubmissionMemoryDao(), new QueueMemoryDao(),
                new RubricConfigMemoryDao(), configurationDao, new RepoUpdateMemoryDao());
    }

    /** Creates database-backed DAOs for the live application. */
    public static DaoService createSqlDAOs() throws DataAccessException {
        SqlDb.setUpDb();
        return new DaoService(
                new UserSqlDao(), new SubmissionSqlDao(), new QueueSqlDao(),
                new RubricConfigSqlDao(), new ConfigurationSqlDao(), new RepoUpdateSqlDao());
    }
}
