package edu.byu.cs.autograder.score.latepenalties;

import edu.byu.cs.autograder.GradingException;
import edu.byu.cs.autograder.score.penalties.PercentPenaltyCalculator;
import edu.byu.cs.model.Submission;
import org.junit.jupiter.api.*;

import edu.byu.cs.dataAccess.DaoService;
import edu.byu.cs.dataAccess.DataAccessException;
import edu.byu.cs.dataAccess.daoInterface.ConfigurationDao;
import edu.byu.cs.model.Rubric;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class PercentPenaltyCalculatorTest extends PenaltyCalculatorTest {
    private float penaltyPerDay;

    @BeforeAll
    static void setUpPercentPenalty() throws DataAccessException {
        setUp();
        penaltyCalculator = new PercentPenaltyCalculator();
    }

    int daysLate;

    @ParameterizedTest
    @MethodSource("getRubrics")
    @Override
    public void testEarlySubmission(Rubric testRubric) throws DataAccessException, GradingException {
        // For this implementation, early submissions are treated as if they were on time,
        // because LateDayCalculator resolves early submissions as 0 days late.
        // Since on-time submissions are already tested, this test is unnecessary in this implementation
        // We will not call testOnTimeSubmission(testRubric) because that would be running a duplicate test.
    }

    @ParameterizedTest
    @MethodSource("getRubrics")
    @Override
    void testOnTimeSubmission(Rubric testRubric) throws DataAccessException, GradingException {
        calculateAndEvaluateScore(testRubric, 0);
    }

    @ParameterizedTest
    @MethodSource("getRubrics")
    @Override
    public void testOneDayLate(Rubric testRubric) throws DataAccessException, GradingException {
        calculateAndEvaluateScore(testRubric, 1);
    }

    @ParameterizedTest
    @MethodSource("getRubrics")
    @Override
    public void testMaxLate(Rubric testRubric) throws DataAccessException, GradingException {
        // The LateDayCalculator is the object that reduces the days late to the maximum late days value,
        // so we can't use an arbitrarily high number in this test.
        daysLate = DaoService.getConfigurationDao().getConfiguration(ConfigurationDao.Configuration.MAX_LATE_DAYS_TO_PENALIZE,  Integer.class);
        calculateAndEvaluateScore(testRubric, daysLate);
    }

    @ParameterizedTest
    @MethodSource("getRubrics")
    @Override
    public void testLatePenaltyNotesFormat(Rubric testRubric) throws DataAccessException, GradingException {
        Submission onTimeSubmission = penaltyCalculator.applyPenalty(testRubric, 0, gradingContext, mockCommitReport);
        daysLate = 1;
        penaltyPerDay = DaoService.getConfigurationDao().getConfiguration(ConfigurationDao.Configuration.PER_DAY_LATE_PENALTY, Float.class);

        Submission lateSubmission = penaltyCalculator.applyPenalty(testRubric, daysLate, gradingContext, mockCommitReport);

        int maxDaysLate = DaoService.getConfigurationDao().getConfiguration(ConfigurationDao.Configuration.MAX_LATE_DAYS_TO_PENALIZE,  Integer.class);

        Submission maxDaysLateSubmission = penaltyCalculator.applyPenalty(testRubric, maxDaysLate, gradingContext, mockCommitReport);

        String testNotes;
        String testLateNotes;
        String testNotesMax;

        for (Rubric.RubricType type: lateSubmission.rubric().items().keySet()) {
            testNotes = onTimeSubmission.rubric().items().get(type).results().notes();
            testLateNotes = lateSubmission.rubric().items().get(type).results().notes();
            testNotesMax = maxDaysLateSubmission.rubric().items().get(type).results().notes();
            String finalTestNotes = testNotes;
            Assertions.assertThrows(AssertionError.class, () -> containsExpected(finalTestNotes, String.format("-%d%%", 0), "late"));
            containsExpected(testLateNotes, String.format("-%d%%", (int) (daysLate * penaltyPerDay * 100)), "late");
            containsExpected(testNotesMax, String.format("-%d%%", (int) (maxDaysLate * penaltyPerDay * 100)), "late", "penalty", "maxed");
        }
    }


    /*
     * Reruns all tests with different values for the late penalty.
     */
    @Override
    @ParameterizedTest
    @MethodSource("getRubrics")
    public void testPenaltyConfigOverride(Rubric testRubric) throws DataAccessException, GradingException {
        ConfigurationDao configurationDao = DaoService.getConfigurationDao();

        // capture original rubric config values so they can be reinserted later to not interfere with later tests
        int origMaxLateDays = configurationDao.getConfiguration(ConfigurationDao.Configuration.MAX_LATE_DAYS_TO_PENALIZE, Integer.class);
        float origLatePenalty = configurationDao.getConfiguration(ConfigurationDao.Configuration.PER_DAY_LATE_PENALTY, Float.class);

        configurationDao.setConfiguration(ConfigurationDao.Configuration.MAX_LATE_DAYS_TO_PENALIZE, 10, Integer.class);
        configurationDao.setConfiguration(ConfigurationDao.Configuration.PER_DAY_LATE_PENALTY, 0.07f, Float.class);

        // the calculator is optimized to only retrieve PER_LATE_DAY_PENALTY on initialization, so we must recreate the object
        penaltyCalculator = new PercentPenaltyCalculator();
        testMaxLate(testRubric);
        testOneDayLate(testRubric);
        testOnTimeSubmission(testRubric);
        testEarlySubmission(testRubric);
        testLatePenaltyNotesFormat(testRubric);

        // reinsert original rubric configuration values
        configurationDao.setConfiguration(ConfigurationDao.Configuration.MAX_LATE_DAYS_TO_PENALIZE, origMaxLateDays, Integer.class);
        configurationDao.setConfiguration(ConfigurationDao.Configuration.PER_DAY_LATE_PENALTY, origLatePenalty, Float.class);
        penaltyCalculator = new PercentPenaltyCalculator();
    }

    /**
     *  ================ helper methods ================
     */


    private void calculateAndEvaluateScore(Rubric rubric, int daysLate) throws DataAccessException, GradingException {
        penaltyPerDay = DaoService.getConfigurationDao().getConfiguration(ConfigurationDao.Configuration.PER_DAY_LATE_PENALTY, Float.class);

        Submission resultSubmission = penaltyCalculator.applyPenalty(rubric, daysLate, gradingContext, mockCommitReport);

        for (Rubric.RubricItem item : resultSubmission.rubric().items().values()){
            Rubric.Results results = item.results();
            Assertions.assertEquals(results.rawScore() * (1 - daysLate * penaltyPerDay), results.score());
        }
    }

}
