package edu.byu.cs.dataAccess;

import edu.byu.cs.dataAccess.daoInterface.*;
import edu.byu.cs.dataAccess.memory.*;
import edu.byu.cs.dataAccess.sql.*;
import edu.byu.cs.model.Phase;
import edu.byu.cs.model.RubricConfig;

import java.util.EnumMap;
import java.util.Map;

import static edu.byu.cs.model.Rubric.RubricType.*;

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

        /* Initialize crucial default values in Config and RubricConfig for testing purposes */
        try {
            configurationDao.setConfiguration(ConfigurationDao.Configuration.GIT_COMMIT_PENALTY, 0.1f, Float.class);
            configurationDao.setConfiguration(ConfigurationDao.Configuration.MAX_LATE_DAYS_TO_PENALIZE, 5, Integer.class);
            configurationDao.setConfiguration(ConfigurationDao.Configuration.PER_DAY_LATE_PENALTY, 0.1f, Float.class);
            configurationDao.setConfiguration(ConfigurationDao.Configuration.LINES_PER_COMMIT_REQUIRED, 5, Integer.class);
            configurationDao.setConfiguration(ConfigurationDao.Configuration.CLOCK_FORGIVENESS_MINUTES, 3, Integer.class);

            rubricConfigDao.setRubricConfig(Phase.GitHub, new RubricConfig(Phase.GitHub, new EnumMap<>(Map.of(
                    GITHUB_REPO, new RubricConfig.RubricConfigItem("GitHub Repository", "Two Git commits: one for creating the repository and another for `notes.md`.", 15, "_6829")
            ))));

            rubricConfigDao.setRubricConfig(Phase.Phase0, new RubricConfig(Phase.Phase0, new EnumMap<>(Map.of(
                    GIT_COMMITS, new RubricConfig.RubricConfigItem("Git Commits", "Necessary commit amount", 0, "90342_649"),
                    PASSOFF_TESTS, new RubricConfig.RubricConfigItem("Functionality", "All pass off test cases succeed", 125, "_1958")
            ))));

            rubricConfigDao.setRubricConfig(Phase.Phase1, new RubricConfig(Phase.Phase1, new EnumMap<>(Map.of(
                    GIT_COMMITS, new RubricConfig.RubricConfigItem("Git Commits", "Necessary commit amount", 0, "90342_7800"),
                    EXTRA_CREDIT, new RubricConfig.RubricConfigItem("Extra Credit", "Castling and En Passant", 10, "90342_7835"),
                    PASSOFF_TESTS, new RubricConfig.RubricConfigItem("Functionality", "All pass off test cases succeed", 125, "_1958")
            ))));

            rubricConfigDao.setRubricConfig(Phase.Phase3, new RubricConfig(Phase.Phase3, new EnumMap<>(Map.of(
                    GIT_COMMITS, new RubricConfig.RubricConfigItem("Git Commits", "Necessary commit amount", 0, "90344_2520"),
                    PASSOFF_TESTS, new RubricConfig.RubricConfigItem("Web API Works", "All pass off test cases in StandardAPITests.java succeed", 125, "_5202"),
                    QUALITY, new RubricConfig.RubricConfigItem("Code Quality", "Chess Code Quality Rubric (see GitHub)", 30, "_3003"),
                    UNIT_TESTS, new RubricConfig.RubricConfigItem(
                            "Unit Tests",
                            "All test cases pass\nEach public method on your Service classes has two test cases, one positive test and one negative test\nEvery test case includes an Assert statement of some type",
                            25,
                            "90344_776")
            ))));

            rubricConfigDao.setRubricConfig(Phase.Phase4, new RubricConfig(Phase.Phase4, new EnumMap<>(Map.of(
                    GIT_COMMITS, new RubricConfig.RubricConfigItem("Git Commits", "Necessary commit amount", 0, "90346_6245"),
                    PASSOFF_TESTS, new RubricConfig.RubricConfigItem("Functionality", "All pass off test cases succeed", 100, "_2614"),
                    QUALITY, new RubricConfig.RubricConfigItem("Code Quality", "Chess Code Quality Rubric (see GitHub)", 30, "90346_8398"),
                    UNIT_TESTS, new RubricConfig.RubricConfigItem(
                            "Unit Tests",
                            "All test cases pass\nEach public method on DAO classes has two test cases, one positive test and one negative test\nEvery test case includes an Assert statement of some type",
                            25,
                            "90346_5755")
            ))));

            rubricConfigDao.setRubricConfig(Phase.Phase5, new RubricConfig(Phase.Phase5, new EnumMap<>(Map.of(
                    GIT_COMMITS, new RubricConfig.RubricConfigItem("Git Commits", "Necessary commit amount", 0, "90347_8497"),
                    QUALITY, new RubricConfig.RubricConfigItem("Code Quality", "Chess Code Quality Rubric (see GitHub)", 30, "90347_9378"),
                    UNIT_TESTS, new RubricConfig.RubricConfigItem(
                            "Unit Tests",
                            "All test cases pass\nEach public method on the Server Facade class has two test cases, one positive test and one negative test\nEvery test case includes an Assert statement of some type",
                            25,
                            "90347_2215")
            ))));

            rubricConfigDao.setRubricConfig(Phase.Phase6, new RubricConfig(Phase.Phase6, new EnumMap<>(Map.of(
                    GIT_COMMITS, new RubricConfig.RubricConfigItem("Git Commits", "Necessary commit amount", 0, "90348_9048"),
                    PASSOFF_TESTS, new RubricConfig.RubricConfigItem(
                            "Automated Pass Off Test Cases",
                            "Each provided test case passed is worth a proportional number of points ((passed / total) * 50).",
                            50,
                            "90348_899"),
                    QUALITY, new RubricConfig.RubricConfigItem("Code Quality", "Chess Code Quality Rubric (see GitHub)", 30, "90348_3792")
            ))));

            rubricConfigDao.setRubricConfig(Phase.Quality, new RubricConfig(Phase.Quality, new EnumMap<>(Map.of(
                    QUALITY, new RubricConfig.RubricConfigItem("Code Quality", "Chess Code Quality Rubric (see GitHub)", 30, null)
            ))));
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
