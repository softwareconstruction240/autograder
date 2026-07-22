package edu.byu.cs.autograder.score.latepenalties;

import org.junit.jupiter.api.*;

import edu.byu.cs.dataAccess.DaoService;
import edu.byu.cs.dataAccess.DataAccessException;
import edu.byu.cs.dataAccess.daoInterface.ConfigurationDao;
import edu.byu.cs.model.Rubric;

public class PercentPenaltyCalculatorTest extends LatePenaltyCalculatorTest {
    private float penaltyPerDay;

    @BeforeAll
    static void setUpPercentPenalty() throws DataAccessException {
        setUp();
        latePenaltyCalculator = new PercentPenaltyCalculator();
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
        penaltyPerDay = DaoService.getConfigurationDao().getConfiguration(ConfigurationDao.Configuration.PER_DAY_LATE_PENALTY, Float.class);

        Rubric resultRubric = latePenaltyCalculator.applyLatePenalty(testRubricOneItem, daysLate, gradingContext);
        Rubric.Results passoffTestResults = resultRubric.items().get(Rubric.RubricType.PASSOFF_TESTS).results();

        Assertions.assertEquals(passoffTestResults.rawScore(), passoffTestResults.score());
    }

    @Test
    @Override
    public void testOneDayLate() throws DataAccessException {
        daysLate = 1;
        penaltyPerDay = DaoService.getConfigurationDao().getConfiguration(ConfigurationDao.Configuration.PER_DAY_LATE_PENALTY, Float.class);

        Rubric resultRubric = latePenaltyCalculator.applyLatePenalty(testRubricOneItem, daysLate, gradingContext);
        Rubric.Results passoffTestResults = resultRubric.items().get(Rubric.RubricType.PASSOFF_TESTS).results();

        Assertions.assertEquals(passoffTestResults.rawScore() * (1 - daysLate * penaltyPerDay), passoffTestResults.score());
    }


    @Test
    @Override
    public void testMaxLate() throws DataAccessException {
        // The LateDayCalculator is the object that reduces the days late to the maximum late days value,
        // so we can't use an arbitrarily high number in this test.
        daysLate = DaoService.getConfigurationDao().getConfiguration(ConfigurationDao.Configuration.MAX_LATE_DAYS_TO_PENALIZE,  Integer.class);
        penaltyPerDay = DaoService.getConfigurationDao().getConfiguration(ConfigurationDao.Configuration.PER_DAY_LATE_PENALTY, Float.class);

        Rubric resultRubric = latePenaltyCalculator.applyLatePenalty(testRubricOneItem, daysLate, gradingContext);
        Rubric.Results passoffTestResults = resultRubric.items().get(Rubric.RubricType.PASSOFF_TESTS).results();

        Assertions.assertEquals(passoffTestResults.rawScore() * (1 - daysLate * penaltyPerDay), passoffTestResults.score());
    }

    @Test
    @Override
    public void testLatePenaltyNotesFormat() throws DataAccessException {
        daysLate = 1;
        penaltyPerDay = DaoService.getConfigurationDao().getConfiguration(ConfigurationDao.Configuration.PER_DAY_LATE_PENALTY, Float.class);

        Rubric resultRubric = latePenaltyCalculator.applyLatePenalty(testRubricOneItem, daysLate, gradingContext);
        String testNotes = resultRubric.items().get(Rubric.RubricType.PASSOFF_TESTS).results().notes();

        containsExpected(testNotes, String.format("-%d%%", (int) (daysLate * penaltyPerDay * 100)), "late");

        daysLate = DaoService.getConfigurationDao().getConfiguration(ConfigurationDao.Configuration.MAX_LATE_DAYS_TO_PENALIZE,  Integer.class);

        resultRubric = latePenaltyCalculator.applyLatePenalty(testRubricOneItem, daysLate, gradingContext);
        testNotes = resultRubric.items().get(Rubric.RubricType.PASSOFF_TESTS).results().notes();


        containsExpected(testNotes, String.format("-%d%%", (int) (daysLate * penaltyPerDay * 100)), "late", "penalty", "maxed");
    }

    @Override
    @Test
    public void testPenaltyConfigOverride() throws DataAccessException {
        ConfigurationDao configurationDao = DaoService.getConfigurationDao();

        // capture original rubric config values so they can be reinserted later to not interfere with later tests
        int origMaxLateDays = configurationDao.getConfiguration(ConfigurationDao.Configuration.MAX_LATE_DAYS_TO_PENALIZE, Integer.class);
        float origLatePenalty = configurationDao.getConfiguration(ConfigurationDao.Configuration.PER_DAY_LATE_PENALTY, Float.class);

        configurationDao.setConfiguration(ConfigurationDao.Configuration.MAX_LATE_DAYS_TO_PENALIZE, 10, Integer.class);
        configurationDao.setConfiguration(ConfigurationDao.Configuration.PER_DAY_LATE_PENALTY, 0.07f, Float.class);

        // the calculator is optimized to only retrieve PER_LATE_DAY_PENALTY on initialization, so we must recreate the object
        latePenaltyCalculator = new PercentPenaltyCalculator();
        testMaxLate();
        testOneDayLate();
        testOnTimeSubmission();
        testEarlySubmission();
        testLatePenaltyNotesFormat();

        // reinsert original rubric configuration values
        configurationDao.setConfiguration(ConfigurationDao.Configuration.MAX_LATE_DAYS_TO_PENALIZE, origMaxLateDays, Integer.class);
        configurationDao.setConfiguration(ConfigurationDao.Configuration.PER_DAY_LATE_PENALTY, origLatePenalty, Float.class);
        latePenaltyCalculator = new PercentPenaltyCalculator();
    }

    @Override
    public void testMultipleRubricItems() throws DataAccessException {

    }


    // helper methods
    private void containsExpected(String container, String... expected){
        for (String pattern : expected) {
            Assertions.assertTrue(container.toLowerCase().contains(pattern), () -> String.format("String did not contain expected value: %s\nSource: %s", pattern, container));
        }
    }
}
