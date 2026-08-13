package edu.byu.cs.autograder.score.latepenalties;

import edu.byu.cs.autograder.GradingException;
import edu.byu.cs.autograder.score.penalties.PercentPenaltyCalculator;
import edu.byu.cs.canvas.CanvasException;
import edu.byu.cs.model.*;
import org.junit.jupiter.api.*;

import edu.byu.cs.dataAccess.DaoService;
import edu.byu.cs.dataAccess.DataAccessException;
import edu.byu.cs.dataAccess.daoInterface.ConfigurationDao;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.fail;

public class PercentPenaltyCalculatorTest extends PenaltyCalculatorTest {
    private float penaltyPerDay;

    private static final int PASSOFF_POSSIBLE_POINTS = 10;
    private static final int CODE_QUALITY_POSSIBLE_POINTS = 7;
    private static final int UNIT_TESTS_POSSIBLE_POINTS = 6;

    @BeforeAll
    static void setUpPercentPenalty() throws DataAccessException {
        setUp();
        RubricConfig phase3RubricConfig = new RubricConfig(
                Phase.Phase3,
                new EnumMap<>(Map.of(
                        Rubric.RubricType.PASSOFF_TESTS, new RubricConfig.RubricConfigItem("testCategory1", "testCriteria1", PASSOFF_POSSIBLE_POINTS, "testRubricId1"),
                        Rubric.RubricType.QUALITY, new RubricConfig.RubricConfigItem("testCategory2", "testCriteria2", CODE_QUALITY_POSSIBLE_POINTS, "testRubricId2"),
                        Rubric.RubricType.UNIT_TESTS, new RubricConfig.RubricConfigItem("testCategory3", "testCriteria3", UNIT_TESTS_POSSIBLE_POINTS, "testRubricId3"),
                        Rubric.RubricType.GIT_COMMITS, new RubricConfig.RubricConfigItem("testCategory4", "testCriteria4", 0, "testRubricId4")
                )));
        DaoService.getRubricConfigDao().setRubricConfig(Phase.Phase3, phase3RubricConfig);
    }


    int daysLate;
    @BeforeEach
    void setUpCalculator() throws DataAccessException{
        this.penaltyCalculator = new PercentPenaltyCalculator();
        //resets the database for each test
        DaoService.initializeMemoryDAOs();
        DaoService.getQueueDao().add(new QueueItem("testNetId", Phase.Phase4, Instant.now(), true));
    }

    @ParameterizedTest
    @MethodSource("getRubrics")
    @Override
    public void testEarlySubmission(Rubric testRubric) throws DataAccessException, GradingException {
        calculateAndEvaluateScore(testRubric, -1);
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

    @Test
    void score_doesNotDecrease_when_higherPriorScore() throws CanvasException, DataAccessException {
        float newestPassoffPoints = PASSOFF_POSSIBLE_POINTS;
        float newestQualityPoints = CODE_QUALITY_POSSIBLE_POINTS - 1;
        float newestUnitTestPoints = UNIT_TESTS_POSSIBLE_POINTS;
        Submission lastSubmission = previousSubmissionHelper(
                new Phase3SubmissionValues(PASSOFF_POSSIBLE_POINTS, 1, 1, -1, true),
                //TODO: change value back to 30 when max late days logic goes into penalty calculator
                new Phase3SubmissionValues(newestPassoffPoints, newestQualityPoints, newestUnitTestPoints, 5, true)
        );

        Assertions.assertNotNull(lastSubmission);
        EnumMap<Rubric.RubricType, Rubric.RubricItem> rubricItems = lastSubmission.rubric().items();

        Assertions.assertEquals(newestPassoffPoints/PASSOFF_POSSIBLE_POINTS, rubricItems.get(Rubric.RubricType.PASSOFF_TESTS).results().score());
        Assertions.assertEquals((newestQualityPoints / 2) / CODE_QUALITY_POSSIBLE_POINTS, rubricItems.get(Rubric.RubricType.QUALITY).results().score());
        Assertions.assertEquals((newestUnitTestPoints / 2) / UNIT_TESTS_POSSIBLE_POINTS, rubricItems.get(Rubric.RubricType.UNIT_TESTS).results().score());
    }

    @Test
    void score_doesDecrease_when_higherPriorRawScore() throws CanvasException, DataAccessException {
        Submission lastSubmission = previousSubmissionHelper(
                new Phase3SubmissionValues(PASSOFF_POSSIBLE_POINTS, 0, UNIT_TESTS_POSSIBLE_POINTS, -1, true),
                //TODO: change value back to 30 when max late days logic goes into penalty calculator
                new Phase3SubmissionValues(PASSOFF_POSSIBLE_POINTS, CODE_QUALITY_POSSIBLE_POINTS, 0, 5, true)
        );

        Assertions.assertNotNull(lastSubmission);
        EnumMap<Rubric.RubricType, Rubric.RubricItem> rubricItems = lastSubmission.rubric().items();

        Assertions.assertEquals(1, rubricItems.get(Rubric.RubricType.PASSOFF_TESTS).results().score());
        Assertions.assertEquals((CODE_QUALITY_POSSIBLE_POINTS / 2.0f) / CODE_QUALITY_POSSIBLE_POINTS, rubricItems.get(Rubric.RubricType.QUALITY).results().score());
        Assertions.assertEquals(0, rubricItems.get(Rubric.RubricType.UNIT_TESTS).results().score());
    }

    @Test
    void score_doesNotDecrease_when_distributedHigherPriorScore() throws CanvasException, DataAccessException {
        Submission lastSubmission = previousSubmissionHelper(
                new Phase3SubmissionValues(PASSOFF_POSSIBLE_POINTS, 0, UNIT_TESTS_POSSIBLE_POINTS, -1, true),
                new Phase3SubmissionValues(PASSOFF_POSSIBLE_POINTS, CODE_QUALITY_POSSIBLE_POINTS, 0, -1, true),
                new Phase3SubmissionValues(PASSOFF_POSSIBLE_POINTS, CODE_QUALITY_POSSIBLE_POINTS, UNIT_TESTS_POSSIBLE_POINTS, 5, true)
        );

        Assertions.assertNotNull(lastSubmission);
        EnumMap<Rubric.RubricType, Rubric.RubricItem> rubricItems = lastSubmission.rubric().items();

        Assertions.assertEquals(1, rubricItems.get(Rubric.RubricType.PASSOFF_TESTS).results().score());
        Assertions.assertEquals(1, rubricItems.get(Rubric.RubricType.QUALITY).results().score());
        Assertions.assertEquals(1, rubricItems.get(Rubric.RubricType.UNIT_TESTS).results().score());
    }

    @Test
    void score_doesDecrease_when_higherPriorScoreOfFailedSubmission() throws CanvasException, DataAccessException {
        Submission lastSubmission = previousSubmissionHelper(
                new Phase3SubmissionValues(0, CODE_QUALITY_POSSIBLE_POINTS, UNIT_TESTS_POSSIBLE_POINTS, -1, false),
                //TODO: change value back to 30 when max late days logic goes into penalty calculator
                new Phase3SubmissionValues(PASSOFF_POSSIBLE_POINTS, CODE_QUALITY_POSSIBLE_POINTS, UNIT_TESTS_POSSIBLE_POINTS, 5, true)
        );

        Assertions.assertNotNull(lastSubmission);
        EnumMap<Rubric.RubricType, Rubric.RubricItem> rubricItems = lastSubmission.rubric().items();

        Assertions.assertEquals((PASSOFF_POSSIBLE_POINTS / 2f) / PASSOFF_POSSIBLE_POINTS, rubricItems.get(Rubric.RubricType.PASSOFF_TESTS).results().score());
        Assertions.assertEquals((CODE_QUALITY_POSSIBLE_POINTS / 2f) / CODE_QUALITY_POSSIBLE_POINTS, rubricItems.get(Rubric.RubricType.QUALITY).results().score());
        Assertions.assertEquals((UNIT_TESTS_POSSIBLE_POINTS / 2f)/ UNIT_TESTS_POSSIBLE_POINTS, rubricItems.get(Rubric.RubricType.UNIT_TESTS).results().score());
    }

    /**
     *  ================ helper methods ================
     */

    record Phase3SubmissionValues(float passoffPoints, float qualityPoints, float unitTestPoints, int daysLate, boolean passed) {}

    private void calculateAndEvaluateScore(Rubric rubric, int daysLate) throws DataAccessException, GradingException {
        penaltyPerDay = DaoService.getConfigurationDao().getConfiguration(ConfigurationDao.Configuration.PER_DAY_LATE_PENALTY, Float.class);

        Submission resultSubmission = penaltyCalculator.applyPenalty(rubric, daysLate, gradingContext, mockCommitReport);

        for (Rubric.RubricItem item : resultSubmission.rubric().items().values()){
            Rubric.Results results = item.results();
            Assertions.assertEquals(results.rawScore() * (1 - daysLate * penaltyPerDay), results.score());
        }
    }

    private Submission previousSubmissionHelper(Phase3SubmissionValues... values) throws DataAccessException, CanvasException {

        for (int i = 0; i < values.length; i++) {
            Phase3SubmissionValues value = values[i];

            Rubric rubric =  constructRubric(value.passoffPoints() / PASSOFF_POSSIBLE_POINTS,
                    value.qualityPoints() / CODE_QUALITY_POSSIBLE_POINTS,
                    value.unitTestPoints() / UNIT_TESTS_POSSIBLE_POINTS,
                    value.passed);
            Submission submission = value.daysLate < 0 ? scoreRubric(rubric, 0) : scoreRubric(rubric, value.daysLate);

            if (i == values.length - 1) {
                return submission;
            }
            else {
                DaoService.getSubmissionDao().insertSubmission(submission);
            }
        }

        return null;
    }

    private Rubric constructRubric(float passoffScore, float qualityScore, float unitTestScore, boolean passed) {
        Rubric.Results passoffResults = new Rubric.Results("testNotes1", passoffScore, PASSOFF_POSSIBLE_POINTS, null, "testTextResults1");
        Rubric.Results qualityResults = new Rubric.Results("testNotes2", qualityScore, CODE_QUALITY_POSSIBLE_POINTS, null, "testTextResults2");
        Rubric.Results unitTestResults = new Rubric.Results("testNotes3", unitTestScore, UNIT_TESTS_POSSIBLE_POINTS, null, "testTextResults3");

        return new Rubric(new EnumMap<>(Map.of(
                Rubric.RubricType.PASSOFF_TESTS, new Rubric.RubricItem("testCategory1", passoffResults, "testCriteria1"),
                Rubric.RubricType.QUALITY, new Rubric.RubricItem("testCategory2", qualityResults, "testCriteria2"),
                Rubric.RubricType.UNIT_TESTS, new Rubric.RubricItem("testCategory3", unitTestResults, "testCriteria3"))),
                passed, "testNotes");
    }

    private Submission scoreRubric(Rubric rubric, int daysLate) {
        try {
            return penaltyCalculator.applyPenalty(rubric, daysLate, gradingContext, mockCommitReport);
        } catch (Exception e) {
            fail("Unexpected exception thrown: ", e);
        }
        return null;
    }

}
