package edu.byu.cs.dataAccess.base;

import edu.byu.cs.dataAccess.DataAccessException;
import edu.byu.cs.dataAccess.daoInterface.ConfigurationDao;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Instant;
import java.util.Random;

public abstract class ConfigurationDaoTest {
    protected ConfigurationDao dao;
    protected abstract ConfigurationDao getConfigurationDao();
    protected abstract void clearConfigurationItems() throws DataAccessException;
    private static Random random = new Random();

    @BeforeEach
    void setup() throws DataAccessException {
        dao = getConfigurationDao();
        clearConfigurationItems();
    }

    @ParameterizedTest
    @EnumSource(ConfigurationDao.Configuration.class)
    void getAndSetValidConfigurations(ConfigurationDao.Configuration key)
            throws ClassNotFoundException, DataAccessException {
        var value = generateDummyDataForKey(key);
        Class clazz = Class.forName(getNameOfTypeForKey(key));
        dao.setConfiguration(key, value, clazz);
        var obtained = dao.getConfiguration(key, clazz);
        Assertions.assertEquals(value, obtained);
    }

    @Test
    void setItemWithInvalidKey(){

    }

    @Test
    void setItemWithInvalidValue(){

    }

    @Test
    void setItemWithDuplicateKey(){

    }

    @Test
    void getItemThatDoesNotExist(){

    }

    /**
     * This function creates dummy data based on the configuration keys. Some keys will have values that could
     * plausibly be seen in a working autograder database. Others are just to assure that the supported types
     * correctly deserialize in the tests. For example STUDENT_SUBMISSIONS_ENABLED may at one point been a boolean,
     * but at the time of writing is an array of phases saved as a string.
     * <br>
     * For more details on how data might actually be stored in the database, you may want to check your development
     * database after changing some of the config values, or check out {@link edu.byu.cs.service.ConfigService}
     * */
    Object generateDummyDataForKey(ConfigurationDao.Configuration key){
        return switch (key){
            case COURSE_NUMBER -> random.nextInt(10000,99999);
            case LINES_PER_COMMIT_REQUIRED, MAX_LATE_DAYS_TO_PENALIZE, CLOCK_FORGIVENESS_MINUTES
                    -> random.nextInt(0, 10);
            case GITHUB_ASSIGNMENT_NUMBER, PHASE0_ASSIGNMENT_NUMBER, PHASE1_ASSIGNMENT_NUMBER, PHASE3_ASSIGNMENT_NUMBER,
                 PHASE4_ASSIGNMENT_NUMBER, PHASE5_ASSIGNMENT_NUMBER, PHASE6_ASSIGNMENT_NUMBER
                    -> random.nextInt(1000000,2000000);
            case MAX_ERROR_OUTPUT_CHARS -> random.nextInt(1, 10000);
            case GRADER_SHUTDOWN_WARNING_MILLISECONDS -> 86400000; //24 hours in milliseconds
            case GIT_COMMIT_PENALTY, PER_DAY_LATE_PENALTY -> random.nextFloat(0, 1);
            case SLACK_LINK, BANNER_LINK, BANNER_COLOR, BANNER_MESSAGE-> "https://slack.com";
            case STUDENT_SUBMISSIONS_ENABLED -> random.nextBoolean();
            case GRADER_SHUTDOWN_DATE, HOLIDAY_LIST, BANNER_EXPIRATION-> Instant.now();
        };
    }

    String getNameOfTypeForKey(ConfigurationDao.Configuration key){
        return switch (key){
            case COURSE_NUMBER, LINES_PER_COMMIT_REQUIRED, MAX_LATE_DAYS_TO_PENALIZE, CLOCK_FORGIVENESS_MINUTES,
                 GITHUB_ASSIGNMENT_NUMBER, PHASE0_ASSIGNMENT_NUMBER, PHASE1_ASSIGNMENT_NUMBER, PHASE3_ASSIGNMENT_NUMBER,
                 PHASE4_ASSIGNMENT_NUMBER, PHASE5_ASSIGNMENT_NUMBER, PHASE6_ASSIGNMENT_NUMBER, MAX_ERROR_OUTPUT_CHARS,
                 GRADER_SHUTDOWN_WARNING_MILLISECONDS -> "java.lang.Integer";
            case GIT_COMMIT_PENALTY, PER_DAY_LATE_PENALTY-> "java.lang.Float";
            case SLACK_LINK, BANNER_LINK, BANNER_COLOR, BANNER_MESSAGE -> "java.lang.String";
            case STUDENT_SUBMISSIONS_ENABLED -> "java.lang.Boolean";
            case GRADER_SHUTDOWN_DATE, HOLIDAY_LIST, BANNER_EXPIRATION -> "java.time.Instant";
        };
    }
}
