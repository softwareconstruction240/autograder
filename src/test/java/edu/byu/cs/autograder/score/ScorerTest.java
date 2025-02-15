package edu.byu.cs.autograder.score;

import edu.byu.cs.autograder.GradingContext;
import edu.byu.cs.autograder.GradingException;
import edu.byu.cs.autograder.GradingObserver;
import edu.byu.cs.autograder.git.CommitVerificationConfig;
import edu.byu.cs.autograder.git.CommitVerificationReport;
import edu.byu.cs.autograder.git.CommitVerificationResult;
import edu.byu.cs.canvas.CanvasException;
import edu.byu.cs.canvas.CanvasIntegration;
import edu.byu.cs.canvas.CanvasService;
import edu.byu.cs.canvas.FakeCanvasIntegration;
import edu.byu.cs.canvas.model.CanvasRubricAssessment;
import edu.byu.cs.canvas.model.CanvasSubmission;
import edu.byu.cs.dataAccess.daoInterface.ConfigurationDao;
import edu.byu.cs.dataAccess.DaoService;
import edu.byu.cs.dataAccess.DataAccessException;
import edu.byu.cs.model.*;
import edu.byu.cs.model.Submission.VerifiedStatus;
import edu.byu.cs.properties.ApplicationProperties;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.File;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ScorerTest {

    GradingObserver mockObserver = null;
    CanvasIntegration spyCanvasIntegration = null;
    GradingContext gradingContext = null;

    private static final int PASSOFF_POSSIBLE_POINTS = 10;
    private static final int CODE_QUALITY_POSSIBLE_POINTS = 7;
    private static final int UNIT_TESTS_POSSIBLE_POINTS = 6;

    private static final CommitVerificationConfig standardCVConfig = new CommitVerificationConfig(10, 3, 0, 10, 3);

    private static final CommitVerificationReport PASSING_COMMIT_VERIFICATION =
            constructCommitVerificationResult(true, false);
    private static final CommitVerificationReport FAILING_COMMIT_VERIFICATION =
            constructCommitVerificationResult(false, false);
    private static final CommitVerificationReport PASSING_CACHED_COMMIT_VERIFICATION =
            constructCommitVerificationResult(true, true);
    private static final CommitVerificationReport FAILING_CACHED_COMMIT_VERIFICATION =
            constructCommitVerificationResult(false, true);


    @BeforeAll
    static void setUpAll() {
        loadApplicationProperties();
    }

    @BeforeEach
    void setUp() throws DataAccessException {
        spyCanvasIntegration = Mockito.spy(new FakeCanvasIntegration());
        CanvasService.setCanvasIntegration(spyCanvasIntegration);

        try {
            when(spyCanvasIntegration.getAssignmentDueDateForStudent(Mockito.anyInt(), Mockito.anyInt())).thenReturn(
                    ZonedDateTime.now().plusDays(1)
            );

            when(spyCanvasIntegration.getSubmission(Mockito.anyInt(), Mockito.anyInt())).thenReturn(
                    new CanvasSubmission("testUrl", new CanvasRubricAssessment(new HashMap<>()), 1f)
            );
        } catch (CanvasException e) {
            fail("Unexpected exception thrown: ", e);
        }

        DaoService.initializeMemoryDAOs();

        RubricConfig phase0RubricConfig = new RubricConfig(
                Phase.Phase0,
                new EnumMap<>(Map.of(Rubric.RubricType.PASSOFF_TESTS,
                        new RubricConfig.RubricConfigItem("testCategory", "testCriteria", PASSOFF_POSSIBLE_POINTS, "testRubricId"),
                        Rubric.RubricType.GIT_COMMITS, new RubricConfig.RubricConfigItem("testCategory2", "testCriteria2", 0, "testRubricId2")
                )));
        RubricConfig phase3RubricConfig = new RubricConfig(
                Phase.Phase3,
                new EnumMap<>(Map.of(
                        Rubric.RubricType.PASSOFF_TESTS, new RubricConfig.RubricConfigItem("testCategory1", "testCriteria1", PASSOFF_POSSIBLE_POINTS, "testRubricId1"),
                        Rubric.RubricType.QUALITY, new RubricConfig.RubricConfigItem("testCategory2", "testCriteria2", CODE_QUALITY_POSSIBLE_POINTS, "testRubricId2"),
                        Rubric.RubricType.UNIT_TESTS, new RubricConfig.RubricConfigItem("testCategory3", "testCriteria3", UNIT_TESTS_POSSIBLE_POINTS, "testRubricId3"),
                        Rubric.RubricType.GIT_COMMITS, new RubricConfig.RubricConfigItem("testCategory4", "testCriteria4", 0, "testRubricId4")
                )));
        DaoService.getRubricConfigDao().setRubricConfig(Phase.Phase0, phase0RubricConfig);
        DaoService.getRubricConfigDao().setRubricConfig(Phase.Phase3, phase3RubricConfig);
        DaoService.getConfigurationDao().setConfiguration(ConfigurationDao.Configuration.PHASE0_ASSIGNMENT_NUMBER, 0, Integer.class);
        DaoService.getConfigurationDao().setConfiguration(ConfigurationDao.Configuration.PHASE3_ASSIGNMENT_NUMBER, 0, Integer.class);

        DaoService.getUserDao().insertUser(new User("testNetId", 123, "testFirst", "testLast", "testRepoUrl", User.Role.STUDENT));
        DaoService.getQueueDao().add(new QueueItem("testNetId", Phase.Phase0, Instant.now(), false));

        mockObserver = Mockito.mock(GradingObserver.class);

        gradingContext = new GradingContext(
                "testNetId", Phase.Phase0, "testPhasesPath", "testStagePath",
                "testRepoUrl", new File(""),
                standardCVConfig, mockObserver, false);


    }

    @Test
    void score__fullPoints() {
        Rubric phase0Rubric = constructRubric(1f);
        Submission submission = scoreRubric(phase0Rubric);

        assertNotNull(submission);
        assertEquals(1, submission.score());
        assertEquals(PASSOFF_POSSIBLE_POINTS, submission.rubric().items().get(Rubric.RubricType.PASSOFF_TESTS).results().score());
        assertEquals(VerifiedStatus.ApprovedAutomatically, submission.verifiedStatus());

    }

    @Test
    void score__partialPoints() {
        Rubric phase0Rubric = constructRubric(.5f);
        Submission submission = scoreRubric(phase0Rubric);

        assertNotNull(submission);
        assertEquals(.5f, submission.score());
        assertEquals(.5f * PASSOFF_POSSIBLE_POINTS, submission.rubric().items().get(Rubric.RubricType.PASSOFF_TESTS).results().score());
        assertEquals(VerifiedStatus.ApprovedAutomatically, submission.verifiedStatus());
    }

    @Test
    void score__extraPoints() {
        Submission submission = scoreRubric(constructRubric(1.5f));

        assertNotNull(submission);
        assertEquals(1.5f, submission.score());
        assertEquals(1.5f * PASSOFF_POSSIBLE_POINTS, submission.rubric().items().get(Rubric.RubricType.PASSOFF_TESTS).results().score());
    }

    @Test
    void score__noPossiblePoints__error() {
        RubricConfig emptyRubricConfig = new RubricConfig(Phase.Phase0, new EnumMap<>(Rubric.RubricType.class));
        setRubricConfig(Phase.Phase0, emptyRubricConfig);

        var scorer = constructScorer();
        var rubric = constructRubric(1f);
        assertThrows(GradingException.class, () -> scorer.score(rubric, PASSING_COMMIT_VERIFICATION));
    }

    @Test
    void score__commitVerification__verified__repeat() {
        Submission submission = scoreRubric(constructRubric(1.0f), PASSING_CACHED_COMMIT_VERIFICATION);
        assertCommitVerificationResults(submission, VerifiedStatus.PreviouslyApproved, false);
    }

    @Test
    void score__commitVerification__notVerified() {
        Submission submission = scoreRubric(constructRubric(1.0f), FAILING_COMMIT_VERIFICATION);
        assertCommitVerificationResults(submission, VerifiedStatus.Unapproved, true);
    }

    @Test
    void score__commitVerification__notVerified__repeat() {
        Submission submission = scoreRubric(constructRubric(1.0f), FAILING_CACHED_COMMIT_VERIFICATION);
        assertCommitVerificationResults(submission, VerifiedStatus.Unapproved, true);
    }

    private void assertCommitVerificationResults(
            Submission submission, VerifiedStatus verifiedStatus, boolean disallowCanvas) {

        assertNotNull(submission);
        assertEquals(1.0f, submission.score());
        assertTrue(submission.passed());
        assertEquals(verifiedStatus, submission.verifiedStatus());
        assertNull(submission.verification());

        if (disallowCanvas) {
            assertNoCanvasGradeSubmitted();
        }
    }

    @Test
    void score__adminSubmission() {
        gradingContext = new GradingContext(
                "testNetId", Phase.Phase0, "testPhasesPath", "testStagePath",
                "testRepoUrl", new File(""),
                standardCVConfig, mockObserver, true);

        Submission submission = scoreRubric(constructRubric(1f));

        assertNotNull(submission);
        assertTrue(submission.admin());

        assertNoCanvasGradeSubmitted();
    }

    @Test
    void score__phaseNotGradeable() {
        RubricConfig phase0RubricConfig = new RubricConfig(
                Phase.Quality,
                new EnumMap<>(Map.of(Rubric.RubricType.QUALITY,
                        new RubricConfig.RubricConfigItem("testCategory", "testCriteria", 30, "testRubricId")))
        );
        setRubricConfig(Phase.Quality, phase0RubricConfig);

        gradingContext = new GradingContext(
                "testNetId", Phase.Quality, "testPhasesPath", "testStagePath",
                "testRepoUrl", new File(""),
                standardCVConfig, mockObserver, false);
        addQueueItem(new QueueItem("testNetId", Phase.Phase0, Instant.now(), true));

        Rubric emptyRubric = new Rubric(new EnumMap<>(Rubric.RubricType.class), true, "testNotes");
        Submission submission = scoreRubric(emptyRubric);

        assertNotNull(submission);
        assertTrue(submission.passed());
        assertEquals(0, submission.score());

        Mockito.verifyNoInteractions(spyCanvasIntegration);
    }

    @Test
    void score_doesNotDecrease_when_higherPriorScore() throws CanvasException, DataAccessException {
        float newestPassoffPoints = PASSOFF_POSSIBLE_POINTS;
        float newestQualityPoints = CODE_QUALITY_POSSIBLE_POINTS - 1;
        float newestUnitTestPoints = UNIT_TESTS_POSSIBLE_POINTS;
        Submission lastSubmission = previousSubmissionHelper(
                new Phase3SubmissionValues(PASSOFF_POSSIBLE_POINTS, 1, 1, -1),
                new Phase3SubmissionValues(newestPassoffPoints, newestQualityPoints, newestUnitTestPoints, 30)
        );

        Assertions.assertNotNull(lastSubmission);
        EnumMap<Rubric.RubricType, Rubric.RubricItem> rubricItems = lastSubmission.rubric().items();

        Assertions.assertEquals(newestPassoffPoints, rubricItems.get(Rubric.RubricType.PASSOFF_TESTS).results().score());
        Assertions.assertEquals(newestQualityPoints / 2, rubricItems.get(Rubric.RubricType.QUALITY).results().score());
        Assertions.assertEquals(newestUnitTestPoints / 2, rubricItems.get(Rubric.RubricType.UNIT_TESTS).results().score());
    }

    @Test
    void score_doesDecrease_when_higherPriorRawScore() throws CanvasException, DataAccessException {
        Submission lastSubmission = previousSubmissionHelper(
                new Phase3SubmissionValues(PASSOFF_POSSIBLE_POINTS, 0, UNIT_TESTS_POSSIBLE_POINTS, -1),
                new Phase3SubmissionValues(PASSOFF_POSSIBLE_POINTS, CODE_QUALITY_POSSIBLE_POINTS, 0, 30)
        );

        Assertions.assertNotNull(lastSubmission);
        EnumMap<Rubric.RubricType, Rubric.RubricItem> rubricItems = lastSubmission.rubric().items();

        Assertions.assertEquals(PASSOFF_POSSIBLE_POINTS, rubricItems.get(Rubric.RubricType.PASSOFF_TESTS).results().score());
        Assertions.assertEquals(CODE_QUALITY_POSSIBLE_POINTS / 2.0f, rubricItems.get(Rubric.RubricType.QUALITY).results().score());
        Assertions.assertEquals(0, rubricItems.get(Rubric.RubricType.UNIT_TESTS).results().score());
    }

    @Test
    void score_doesNotDecrease_when_distributedHigherPriorScore() throws CanvasException, DataAccessException {
        Submission lastSubmission = previousSubmissionHelper(
                new Phase3SubmissionValues(PASSOFF_POSSIBLE_POINTS, 0, UNIT_TESTS_POSSIBLE_POINTS, -1),
                new Phase3SubmissionValues(PASSOFF_POSSIBLE_POINTS, CODE_QUALITY_POSSIBLE_POINTS, 0, -1),
                new Phase3SubmissionValues(PASSOFF_POSSIBLE_POINTS, CODE_QUALITY_POSSIBLE_POINTS, UNIT_TESTS_POSSIBLE_POINTS, 5)
        );

        Assertions.assertNotNull(lastSubmission);
        EnumMap<Rubric.RubricType, Rubric.RubricItem> rubricItems = lastSubmission.rubric().items();

        Assertions.assertEquals(PASSOFF_POSSIBLE_POINTS, rubricItems.get(Rubric.RubricType.PASSOFF_TESTS).results().score());
        Assertions.assertEquals(CODE_QUALITY_POSSIBLE_POINTS, rubricItems.get(Rubric.RubricType.QUALITY).results().score());
        Assertions.assertEquals(UNIT_TESTS_POSSIBLE_POINTS, rubricItems.get(Rubric.RubricType.UNIT_TESTS).results().score());
    }

    @Test
    void score_doesDecrease_when_higherPriorScoreOfFailedSubmission() throws CanvasException, DataAccessException {
        Submission lastSubmission = previousSubmissionHelper(
                new Phase3SubmissionValues(0, CODE_QUALITY_POSSIBLE_POINTS, UNIT_TESTS_POSSIBLE_POINTS, -1),
                new Phase3SubmissionValues(PASSOFF_POSSIBLE_POINTS, CODE_QUALITY_POSSIBLE_POINTS, UNIT_TESTS_POSSIBLE_POINTS, 30)
        );

        Assertions.assertNotNull(lastSubmission);
        EnumMap<Rubric.RubricType, Rubric.RubricItem> rubricItems = lastSubmission.rubric().items();

        Assertions.assertEquals(PASSOFF_POSSIBLE_POINTS / 2f, rubricItems.get(Rubric.RubricType.PASSOFF_TESTS).results().score());
        Assertions.assertEquals(CODE_QUALITY_POSSIBLE_POINTS / 2f, rubricItems.get(Rubric.RubricType.QUALITY).results().score());
        Assertions.assertEquals(UNIT_TESTS_POSSIBLE_POINTS / 2f, rubricItems.get(Rubric.RubricType.UNIT_TESTS).results().score());
    }

    // Helper Methods for constructing

    private Scorer constructScorer() {
        return new Scorer(gradingContext, new LateDayCalculator());
    }
    /**
     * Helper method to create a Rubric object with the given expected percent, based on PASSOFF_POSSIBLE_POINTS
     *
     * @param score score, 0-1
     * @return the Rubric object
     */
    private Rubric constructRubric(float score) {
        Rubric.Results results = new Rubric.Results(
                "testNotes",
                score,
                PASSOFF_POSSIBLE_POINTS,
                null,
                "testTextResults"
        );

        return new Rubric(new EnumMap<>(Map.of(Rubric.RubricType.PASSOFF_TESTS,
                        new Rubric.RubricItem("testCategory", results, "testCriteria"))),
                true, "testNotes");
    }

    private void setRubricConfig(Phase phase, RubricConfig rubricConfig) {
        try {
            DaoService.getRubricConfigDao().setRubricConfig(phase, rubricConfig);
        } catch (DataAccessException e) {
            fail("Unexpected exception thrown: ", e);
        }
    }
    private void addQueueItem(QueueItem item) {
        try {
            DaoService.getQueueDao().add(item);
        } catch (DataAccessException e) {
            fail("Unexpected exception thrown: ", e);
        }
    }

    private Submission scoreRubric(Rubric rubric) {
        return scoreRubric(rubric, PASSING_COMMIT_VERIFICATION);
    }
    private Submission scoreRubric(Rubric rubric, CommitVerificationReport commitVerification) {
        Scorer scorer = constructScorer();
        return scoreRubric(scorer, rubric, commitVerification);
    }
    private Submission scoreRubric(Scorer scorer, Rubric rubric, CommitVerificationReport commitVerification) {
        try {
            return scorer.score(rubric, commitVerification);
        } catch (Exception e) {
            fail("Unexpected exception thrown: ", e);
        }
        return null;
    }

    private static CommitVerificationReport constructCommitVerificationResult(boolean verified, boolean isCached) {
        String statusStr = verified ? "PASSING" : "FAILING";
        if (isCached) statusStr += "_CACHED";
        String headHash = "<" + statusStr + "_COMMIT_VERIFICATION>";

        return new CommitVerificationResult(
                verified, isCached, 0, 0, 0, false, 0,
                "", null, null, null,
                headHash, null)
                .toReport(null);
    }

    record Phase3SubmissionValues(float passoffPoints, float qualityPoints, float unitTestPoints, int daysLate) {}

    private Submission previousSubmissionHelper(Phase3SubmissionValues... values) throws DataAccessException, CanvasException {
        gradingContext = new GradingContext(
                "testNetId", Phase.Phase3, "testPhasesPath", "testStagePath",
                "testRepoUrl", new File(""),
                standardCVConfig, mockObserver, false);

        for (int i = 0; i < values.length; i++) {
            Phase3SubmissionValues value = values[i];
            when(spyCanvasIntegration.getAssignmentDueDateForStudent(Mockito.anyInt(), Mockito.anyInt())).thenReturn(
                    ZonedDateTime.now().minusDays(value.daysLate())
            );

            Rubric rubric =  constructRubric(value.passoffPoints() / PASSOFF_POSSIBLE_POINTS,
                    value.qualityPoints() / CODE_QUALITY_POSSIBLE_POINTS,
                    value.unitTestPoints() / UNIT_TESTS_POSSIBLE_POINTS);
            Submission submission = scoreRubric(rubric);

            if (i == values.length - 1) {
                return submission;
            }
            else {
                DaoService.getSubmissionDao().insertSubmission(submission);
            }
        }

        return null;
    }

    private Rubric constructRubric(float passoffScore, float qualityScore, float unitTestScore) {
        Rubric.Results passoffResults = new Rubric.Results("testNotes1", passoffScore, PASSOFF_POSSIBLE_POINTS, null, "testTextResults1");
        Rubric.Results qualityResults = new Rubric.Results("testNotes2", qualityScore, CODE_QUALITY_POSSIBLE_POINTS, null, "testTextResults2");
        Rubric.Results unitTestResults = new Rubric.Results("testNotes3", unitTestScore, UNIT_TESTS_POSSIBLE_POINTS, null, "testTextResults3");

        return new Rubric(new EnumMap<>(Map.of(
                Rubric.RubricType.PASSOFF_TESTS, new Rubric.RubricItem("testCategory1", passoffResults, "testCriteria1"),
                Rubric.RubricType.QUALITY, new Rubric.RubricItem("testCategory2", qualityResults, "testCriteria2"),
                Rubric.RubricType.UNIT_TESTS, new Rubric.RubricItem("testCategory3", unitTestResults, "testCriteria3"))),
                true, "testNotes");
    }

    // Assertion Helpers

    private void assertNoCanvasGradeSubmitted() {
        try {
            Mockito.verify(spyCanvasIntegration, never()).submitGrade(anyInt(), anyInt(), anyFloat(), anyString());
            Mockito.verify(spyCanvasIntegration, never()).submitGrade(anyInt(), anyInt(), any(CanvasRubricAssessment.class), anyString());
        } catch (CanvasException e) {
            throw new RuntimeException(e);
        }
    }

    // Loading properties

    private static void loadApplicationProperties() {
        Properties testProperties = new Properties();
        testProperties.setProperty("db-host", "testHost");
        testProperties.setProperty("db-port", "123");
        testProperties.setProperty("db-name", "testName");
        testProperties.setProperty("db-user", "testUser");
        testProperties.setProperty("db-pass", "testPass");
        testProperties.setProperty("frontend-url", "testFrontend");
        testProperties.setProperty("cas-callback-url", "testCallback");
        testProperties.setProperty("canvas-token", "testToken");
        testProperties.setProperty("use-canvas", "true");

        ApplicationProperties.loadProperties(testProperties);
    }
}
