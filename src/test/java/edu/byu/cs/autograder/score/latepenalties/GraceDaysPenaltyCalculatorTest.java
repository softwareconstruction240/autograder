package edu.byu.cs.autograder.score.latepenalties;

import edu.byu.cs.autograder.GradingException;
import edu.byu.cs.autograder.score.penalties.GraceDayPenaltyCalculator;
import edu.byu.cs.canvas.*;
import edu.byu.cs.dataAccess.DaoService;
import edu.byu.cs.dataAccess.DataAccessException;
import edu.byu.cs.dataAccess.daoInterface.ConfigurationDao;
import edu.byu.cs.model.Phase;
import edu.byu.cs.model.Rubric;
import edu.byu.cs.model.Submission;
import edu.byu.cs.properties.ApplicationProperties;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Instant;
import java.util.EnumMap;
import java.util.Properties;


public class GraceDaysPenaltyCalculatorTest extends PenaltyCalculatorTest {
    private static GraceDayPenaltyCalculator graceDayPenaltyCalculator;
    private static FakeGraceDaysCanvasIntegration canvasIntegration;
    static protected Rubric halfCreditRubric;

    @BeforeAll
    public static void setUpGraceDaysPenalty() throws DataAccessException, CanvasException, GradingException {
        setUp();
        Properties props = new Properties();
        props.setProperty("use-canvas", "false");
        ApplicationProperties.loadProperties(props);
        canvasIntegration = new FakeGraceDaysCanvasIntegration();
        CanvasService.setCanvasIntegration(canvasIntegration);
        graceDayPenaltyCalculator = new GraceDayPenaltyCalculator(CanvasService.getCanvasIntegration().getTestStudent().canvasUserId());

        EnumMap<Rubric.RubricType, Rubric.RubricItem> halfCreditItems = new EnumMap<>(Rubric.RubricType.class);
        Rubric.Results halfCreditResults = new Rubric.Results("", 5f, 10, null, "");
        Rubric.RubricItem halfCreditRubricItem =  new Rubric.RubricItem("testCategory", halfCreditResults, "testCriteria");
        halfCreditItems.put(Rubric.RubricType.PASSOFF_TESTS, halfCreditRubricItem);
        halfCreditItems.put(Rubric.RubricType.QUALITY, halfCreditRubricItem);
        halfCreditItems.put(Rubric.RubricType.UNIT_TESTS, halfCreditRubricItem);
        halfCreditRubric = new Rubric(halfCreditItems, true, "");
    }

    @BeforeEach
    public void resetGraceDays() throws DataAccessException {
        canvasIntegration.setGraceDays(0);
        DaoService.getSubmissionDao().removeSubmissionsByNetId(gradingContext.netId(), 0);
    }

    @Override
    @ParameterizedTest
    @MethodSource("getRubrics")
    void testEarlySubmission(Rubric testRubric) throws DataAccessException, GradingException {
        calculateAndEvaluateScore(testRubric, -2, 0);
        canvasIntegration.setGraceDays(5);
        calculateAndEvaluateScore(testRubric, -1, 5);
    }

    @ParameterizedTest
    @MethodSource("getRubrics")
    void testLaterEarlySubmissionSubtractsGraceDays(Rubric testRubric) throws DataAccessException, GradingException {
        int startingGraceDays = 2;
        int daysEarlyFirstSubmission = 2;
        int daysEarlySecondSubmission = 1;
        DaoService.getSubmissionDao().insertSubmission(new Submission(gradingContext.netId(), "", "", Instant.now(), gradingContext.phase(), true, 5f, 5f, null, halfCreditRubric, false, null,null, null, null, 2));
        canvasIntegration.setGraceDays(startingGraceDays + daysEarlyFirstSubmission);

        Submission resultSubmission = graceDayPenaltyCalculator.applyPenalty(testRubric, -daysEarlySecondSubmission, gradingContext, mockCommitReport);
        int expectedEffectiveDaysLate = daysEarlyFirstSubmission - daysEarlySecondSubmission;
        Assertions.assertEquals(-expectedEffectiveDaysLate, resultSubmission.graceDaysEarned(), "Did not earn/deduct correct number of grace days");

        int totalGraceDaysAfterSubmission = canvasIntegration.getGraceDays();
        int expectedGraceDaysLeft = startingGraceDays + daysEarlyFirstSubmission - expectedEffectiveDaysLate;
        Assertions.assertEquals(expectedGraceDaysLeft, totalGraceDaysAfterSubmission, "Incorrect number of grace days saved to canvas");

        for (Rubric.RubricItem item : resultSubmission.rubric().items().values()){
            Rubric.Results results = item.results();
                Assertions.assertEquals(results.rawScore(), results.score(), "Deducted from score when student had sufficient grace days");
        }
    }

    @Override
    @ParameterizedTest
    @MethodSource("getRubrics")
    void testOnTimeSubmission(Rubric testRubric) throws DataAccessException, GradingException {
        calculateAndEvaluateScore(testRubric, 0, 0);
        canvasIntegration.setGraceDays(5);
        calculateAndEvaluateScore(testRubric, 0, 5);
    }

    @Override
    @ParameterizedTest
    @MethodSource("getRubrics")
    void testOneDayLate(Rubric testRubric) throws DataAccessException, GradingException {
        calculateAndEvaluateScore(testRubric, 1, 0);
        canvasIntegration.setGraceDays(5);
        calculateAndEvaluateScore(testRubric, 1, 5);
    }

    @ParameterizedTest
    @MethodSource("getRubrics")
    void testLateSubmissionAfterEarlySubmissionSubtractsAdditionalGraceDays(Rubric testRubric) throws DataAccessException, GradingException {
        // correct number of grace days are stored in canvas but not correct number are stored in the submission as graceDaysEarned
        int startingGraceDays = 7;
        int daysEarlyFirstSubmission = 2;
        int daysLateSecondSubmission = 3;
        DaoService.getSubmissionDao().insertSubmission(new Submission(gradingContext.netId(), "", "", Instant.now(), gradingContext.phase(), true, 5f, 5f, null, halfCreditRubric, false, null,null, null, null, 2));
        canvasIntegration.setGraceDays(startingGraceDays + daysEarlyFirstSubmission);

        Submission resultSubmission = graceDayPenaltyCalculator.applyPenalty(testRubric, daysLateSecondSubmission, gradingContext, mockCommitReport);
        int expectedEffectiveDaysLate = daysEarlyFirstSubmission + daysLateSecondSubmission;
        Assertions.assertEquals(-expectedEffectiveDaysLate, resultSubmission.graceDaysEarned(), "Did not earn/deduct correct number of grace days");

        int totalGraceDaysAfterSubmission = canvasIntegration.getGraceDays();
        int expectedGraceDaysLeft = startingGraceDays + daysEarlyFirstSubmission - expectedEffectiveDaysLate;
        Assertions.assertEquals(expectedGraceDaysLeft, totalGraceDaysAfterSubmission, "Incorrect number of grace days saved to canvas");

        for (Rubric.RubricItem item : resultSubmission.rubric().items().values()){
            Rubric.Results results = item.results();
            Assertions.assertEquals(results.rawScore(), results.score(), "Deducted from score when student had sufficient grace days");
        }
    }

    @Override
    @ParameterizedTest
    @MethodSource("getRubrics")
    void testMaxLate(Rubric testRubric) throws DataAccessException, GradingException {
        // There are no max days late, so I'll just throw in 1000 and make sure it works because it shouldn't ever get anywhere close to that
        canvasIntegration.setGraceDays(10);
        calculateAndEvaluateScore(testRubric, 1000, 10);
    }

    @Override
    @ParameterizedTest
    @MethodSource("getRubrics")
    @Disabled
    public void testPenaltyConfigOverride(Rubric testRubric) throws DataAccessException, GradingException {
        // wait to test until/if configurable settings are established.
    }

    @Override
    @ParameterizedTest
    @MethodSource("getRubrics")
    void testLatePenaltyNotesFormat(Rubric testRubric) throws DataAccessException, GradingException {
        Submission earlySubmission = graceDayPenaltyCalculator.applyPenalty(testRubric, -3, gradingContext, mockCommitReport);
        canvasIntegration.setGraceDays(0);
        Submission onTimeSubmission = graceDayPenaltyCalculator.applyPenalty(testRubric, 0, gradingContext, mockCommitReport);
        canvasIntegration.setGraceDays(3);
        Submission lateSubmission = graceDayPenaltyCalculator.applyPenalty(testRubric, 3, gradingContext, mockCommitReport);
        canvasIntegration.setGraceDays(1);
        Submission insufficientGraceDaysSubmission = graceDayPenaltyCalculator.applyPenalty(testRubric, 100, gradingContext, mockCommitReport);

        String testNotesEarly = earlySubmission.rubric().notes();
        String testNotesOnTime = onTimeSubmission.rubric().notes();
        String testNotesLate = lateSubmission.rubric().notes();
        String testNotesMax = insufficientGraceDaysSubmission.rubric().notes();
        Assertions.assertThrows(AssertionError.class, () -> containsExpected(testNotesEarly, String.format("-%d%%", 0), "late"));
        Assertions.assertThrows(AssertionError.class, () -> containsExpected(testNotesOnTime, String.format("-%d%%", 0), "late"));
        containsExpected(testNotesEarly, "early", "grace day");
        containsExpected(testNotesOnTime, "on time", "unaffected");
        containsExpected(testNotesLate, "late", "grace day");
        containsExpected(testNotesMax, "zero", "not enough", "grace day", "unaffected");

        // Test resubmission after early another early submission: notes should explain grace day deduction relative to previous submission
        DaoService.getSubmissionDao().insertSubmission(new Submission(gradingContext.netId(), "", "", Instant.now(), gradingContext.phase(), true, 5f, 5f, null, halfCreditRubric, false, null, null, null, null, 3));
        canvasIntegration.setGraceDays(5);
        Submission resubmissionAfterEarly = graceDayPenaltyCalculator.applyPenalty(testRubric, -1, gradingContext, mockCommitReport);
        String testNotesResubmission = resubmissionAfterEarly.rubric().notes();
        containsExpected(testNotesResubmission, "late", "previous", "grace day");
    }

    private void calculateAndEvaluateScore(Rubric rubric, int daysLate, int graceDays) throws DataAccessException, GradingException {
        float insufficientGraceDaysPenalty = 1f;

        Submission resultSubmission = graceDayPenaltyCalculator.applyPenalty(rubric, daysLate, gradingContext, mockCommitReport);
        Assertions.assertEquals(-daysLate, resultSubmission.graceDaysEarned(), "Did not earn/deduct correct number of grace days");

        int totalGraceDaysAfterSubmission = canvasIntegration.getGraceDays();
        int expectedGraceDaysLeft = graceDays - daysLate;
        if (graceDays - daysLate < 0) {
            expectedGraceDaysLeft = graceDays;
        }
        Assertions.assertEquals(expectedGraceDaysLeft, totalGraceDaysAfterSubmission, "Incorrect number of grace days saved to canvas");

        for (Rubric.RubricItem item : resultSubmission.rubric().items().values()){

            Rubric.Results results = item.results();
            if (graceDays >= daysLate) {
                Assertions.assertEquals(results.rawScore(), results.score(), "Deducted from score when student had sufficient grace days");
            } else {
                Assertions.assertEquals(results.rawScore() * (1 - insufficientGraceDaysPenalty), results.score(), "Did not deduct from score when student had insufficient grace days");
            }
        }
    }
}
