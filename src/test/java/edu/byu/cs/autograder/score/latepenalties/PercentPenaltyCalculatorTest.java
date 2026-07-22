package edu.byu.cs.autograder.score.latepenalties;

import org.junit.jupiter.api.*;

import edu.byu.cs.dataAccess.DaoService;
import edu.byu.cs.dataAccess.DataAccessException;
import edu.byu.cs.dataAccess.daoInterface.ConfigurationDao;
import edu.byu.cs.model.Rubric;

public class PercentPenaltyCalculatorTest extends LatePenaltyCalculatorTest {
    private static float penaltyPerDay;

    @BeforeAll
    static void setUpPercentPenalty() throws DataAccessException {
        setUp();
        latePenaltyCalculator = new PercentPenaltyCalculator();
        penaltyPerDay = DaoService.getConfigurationDao().getConfiguration(ConfigurationDao.Configuration.PER_DAY_LATE_PENALTY, Float.class);
    }

    int daysLate;

    @Test
    @Override
    public void testEarlySubmission() throws DataAccessException {
        // For this implementation, early submissions are treated as if they were on time
        testOnTimeSubmission();
    }

    @Test
    @Override
    void testOnTimeSubmission() throws DataAccessException {
        daysLate = 0;

        Rubric resultRubric = latePenaltyCalculator.applyLatePenalty(testRubric, daysLate, gradingContext);
        Rubric.Results passoffTestResults = resultRubric.items().get(Rubric.RubricType.PASSOFF_TESTS).results();

        Assertions.assertEquals(passoffTestResults.rawScore(), passoffTestResults.score());
    }

    @Test
    @Override
    public void testOneDayLate() throws DataAccessException {
        daysLate = 1;
        
        Rubric resultRubric = latePenaltyCalculator.applyLatePenalty(testRubric, daysLate, gradingContext);
        Rubric.Results passoffTestResults = resultRubric.items().get(Rubric.RubricType.PASSOFF_TESTS).results();

        Assertions.assertEquals(passoffTestResults.rawScore() * (1 - daysLate * penaltyPerDay), passoffTestResults.score());
    }


    @Test
    @Override
    public void testOneWeekLate() throws DataAccessException {
        daysLate = 7;

        Rubric resultRubric = latePenaltyCalculator.applyLatePenalty(testRubric, daysLate, gradingContext);
        Rubric.Results passoffTestResults = resultRubric.items().get(Rubric.RubricType.PASSOFF_TESTS).results();

        Assertions.assertEquals(passoffTestResults.rawScore() * (1 - maxDaysPenaliized * penaltyPerDay), passoffTestResults.score());
    }

    @Test
    @Override
    public void testOneMonthLate() throws DataAccessException {
        daysLate = 30;

        Rubric resultRubric = latePenaltyCalculator.applyLatePenalty(testRubric, daysLate, gradingContext);
        Rubric.Results passoffTestResults = resultRubric.items().get(Rubric.RubricType.PASSOFF_TESTS).results();

        Assertions.assertEquals(passoffTestResults.rawScore() * (1 - maxDaysPenaliized * penaltyPerDay), passoffTestResults.score());

    }

    @Test
    @Disabled
    public void testPenaltyConfigOverride() throws DataAccessException {
        ConfigurationDao configurationDao = DaoService.getConfigurationDao();
        // capture original rubric config values so they can be reinserted later to not interfere with later tests
        int origMaxLateDays = configurationDao.getConfiguration(ConfigurationDao.Configuration.MAX_LATE_DAYS_TO_PENALIZE, Integer.class);
        int origLatePenalty = configurationDao.getConfiguration(ConfigurationDao.Configuration.PER_DAY_LATE_PENALTY, Integer.class);

        configurationDao.setConfiguration(ConfigurationDao.Configuration.MAX_LATE_DAYS_TO_PENALIZE, 10, Integer.class);
        configurationDao.setConfiguration(ConfigurationDao.Configuration.PER_DAY_LATE_PENALTY, 7, Integer.class);

        // reinsert original rubric configuration values
        configurationDao.setConfiguration(ConfigurationDao.Configuration.MAX_LATE_DAYS_TO_PENALIZE, origMaxLateDays, Integer.class);
        configurationDao.setConfiguration(ConfigurationDao.Configuration.PER_DAY_LATE_PENALTY, origLatePenalty, Integer.class);
    }

    @Test
    @Override
    @Disabled
    public void testLatePenaltyNotesFormat() throws DataAccessException {

    }
}
