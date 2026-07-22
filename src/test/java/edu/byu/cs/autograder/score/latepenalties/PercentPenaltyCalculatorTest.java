package edu.byu.cs.autograder.score.latepenalties;

import org.junit.jupiter.api.*;

import edu.byu.cs.dataAccess.DaoService;
import edu.byu.cs.dataAccess.DataAccessException;
import edu.byu.cs.dataAccess.daoInterface.ConfigurationDao;
import edu.byu.cs.model.Rubric;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;

public class PercentPenaltyCalculatorTest extends LatePenaltyCalculatorTest {
    private float penaltyPerDay;

    @BeforeAll
    static void setUpPercentPenalty() throws DataAccessException {
        setUp();
        latePenaltyCalculator = new PercentPenaltyCalculator();
    }

    int daysLate;

    @ParameterizedTest
    @MethodSource("getRubrics")
    @Override
    public void testEarlySubmission(Rubric testRubric) throws DataAccessException {
        // For this implementation, early submissions are treated as if they were on time,
        // because LateDayCalculator resolves early submissions as 0 days late.
        testOnTimeSubmission(testRubric);
    }

    @ParameterizedTest
    @MethodSource("getRubrics")
    @Override
    void testOnTimeSubmission(Rubric testRubric) throws DataAccessException {
        calculateAndEvaluateScore(testRubricOneItem, 0);
    }

    @ParameterizedTest
    @MethodSource("getRubrics")
    @Override
    public void testOneDayLate(Rubric testRubric) throws DataAccessException {
        calculateAndEvaluateScore(testRubricOneItem, 1);
    }


    @ParameterizedTest
    @MethodSource("getRubrics")
    @Override
    public void testMaxLate(Rubric testRubric) throws DataAccessException {
        // The LateDayCalculator is the object that reduces the days late to the maximum late days value,
        // so we can't use an arbitrarily high number in this test.
        daysLate = DaoService.getConfigurationDao().getConfiguration(ConfigurationDao.Configuration.MAX_LATE_DAYS_TO_PENALIZE,  Integer.class);
        calculateAndEvaluateScore(testRubricOneItem, daysLate);
    }

    @ParameterizedTest
    @MethodSource("getRubrics")
    @Override
    public void testLatePenaltyNotesFormat(Rubric testRubric) throws DataAccessException {
        daysLate = 1;
        penaltyPerDay = DaoService.getConfigurationDao().getConfiguration(ConfigurationDao.Configuration.PER_DAY_LATE_PENALTY, Float.class);

        Rubric resultRubric = latePenaltyCalculator.applyLatePenalty(testRubricOneItem, daysLate, gradingContext);

        int maxDaysLate = DaoService.getConfigurationDao().getConfiguration(ConfigurationDao.Configuration.MAX_LATE_DAYS_TO_PENALIZE,  Integer.class);

        Rubric resultRubricMax = latePenaltyCalculator.applyLatePenalty(testRubricOneItem, maxDaysLate, gradingContext);

        String testNotes;
        String testNotesMax;

        for (Rubric.RubricType type: resultRubric.items().keySet()) {
            testNotes = resultRubric.items().get(type).results().notes();
            testNotesMax = resultRubricMax.items().get(type).results().notes();
            containsExpected(testNotes, String.format("-%d%%", (int) (daysLate * penaltyPerDay * 100)), "late");
            containsExpected(testNotesMax, String.format("-%d%%", (int) (maxDaysLate * penaltyPerDay * 100)), "late", "penalty", "maxed");
        }
    }

    @Override
    @ParameterizedTest
    @MethodSource("getRubrics")
    public void testPenaltyConfigOverride(Rubric testRubric) throws DataAccessException {
        ConfigurationDao configurationDao = DaoService.getConfigurationDao();

        // capture original rubric config values so they can be reinserted later to not interfere with later tests
        int origMaxLateDays = configurationDao.getConfiguration(ConfigurationDao.Configuration.MAX_LATE_DAYS_TO_PENALIZE, Integer.class);
        float origLatePenalty = configurationDao.getConfiguration(ConfigurationDao.Configuration.PER_DAY_LATE_PENALTY, Float.class);

        configurationDao.setConfiguration(ConfigurationDao.Configuration.MAX_LATE_DAYS_TO_PENALIZE, 10, Integer.class);
        configurationDao.setConfiguration(ConfigurationDao.Configuration.PER_DAY_LATE_PENALTY, 0.07f, Float.class);

        // the calculator is optimized to only retrieve PER_LATE_DAY_PENALTY on initialization, so we must recreate the object
        latePenaltyCalculator = new PercentPenaltyCalculator();
        testMaxLate(testRubric);
        testOneDayLate(testRubric);
        testOnTimeSubmission(testRubric);
        testEarlySubmission(testRubric);
        testLatePenaltyNotesFormat(testRubric);

        // reinsert original rubric configuration values
        configurationDao.setConfiguration(ConfigurationDao.Configuration.MAX_LATE_DAYS_TO_PENALIZE, origMaxLateDays, Integer.class);
        configurationDao.setConfiguration(ConfigurationDao.Configuration.PER_DAY_LATE_PENALTY, origLatePenalty, Float.class);
        latePenaltyCalculator = new PercentPenaltyCalculator();
    }

    /**
     *  ================ helper methods ================
     */

    private void containsExpected(String container, String... expected){
        for (String pattern : expected) {
            Assertions.assertTrue(container.toLowerCase().contains(pattern), () -> String.format("String did not contain expected value: %s\nSource: %s", pattern, container));
        }
    }

    private void calculateAndEvaluateScore(Rubric rubric, int daysLate) throws DataAccessException{
        penaltyPerDay = DaoService.getConfigurationDao().getConfiguration(ConfigurationDao.Configuration.PER_DAY_LATE_PENALTY, Float.class);

        Rubric resultRubric = latePenaltyCalculator.applyLatePenalty(rubric, daysLate, gradingContext);

        for (Rubric.RubricItem item : resultRubric.items().values()){
            Rubric.Results results = item.results();
            Assertions.assertEquals(results.rawScore() * (1 - daysLate * penaltyPerDay), results.score());
        }
    }

}
