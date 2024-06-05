package edu.byu.cs.autograder.score;

import edu.byu.cs.autograder.GradingContext;
import edu.byu.cs.autograder.GradingException;
import edu.byu.cs.autograder.GradingObserver;
import edu.byu.cs.autograder.git.CommitVerificationConfig;
import edu.byu.cs.autograder.git.CommitVerificationResult;
import edu.byu.cs.canvas.CanvasException;
import edu.byu.cs.canvas.CanvasIntegration;
import edu.byu.cs.canvas.CanvasService;
import edu.byu.cs.canvas.FakeCanvasIntegration;
import edu.byu.cs.canvas.model.CanvasRubricAssessment;
import edu.byu.cs.canvas.model.CanvasSubmission;
import edu.byu.cs.dataAccess.DaoService;
import edu.byu.cs.dataAccess.DataAccessException;
import edu.byu.cs.dataAccess.memory.*;
import edu.byu.cs.model.*;
import edu.byu.cs.properties.ApplicationProperties;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import edu.byu.cs.model.Submission.VerifiedStatus;

import java.io.File;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ScorerTest {

    GradingObserver mockObserver = null;
    CanvasIntegration spyCanvasIntegration = null;
    GradingContext gradingContext = null;

    private static final int PASSOFF_POSSIBLE_POINTS = 10;

    private static final CommitVerificationConfig standardCVConfig = new CommitVerificationConfig(10, 3, 0, 10, 3);

    private static final CommitVerificationResult PASSING_COMMIT_VERIFICATION =
            constructCommitVerificationResult(true, false);
    private static final CommitVerificationResult FAILING_COMMIT_VERIFICATION =
            constructCommitVerificationResult(false, false);
    private static final CommitVerificationResult PASSING_CACHED_COMMIT_VERIFICATION =
            constructCommitVerificationResult(true, true);
    private static final CommitVerificationResult FAILING_CACHED_COMMIT_VERIFICATION =
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
                new RubricConfig.RubricConfigItem("testCategory", "testCriteria", PASSOFF_POSSIBLE_POINTS),
                null,
                null
        );
        DaoService.getRubricConfigDao().setRubricConfig(Phase.Phase0, phase0RubricConfig);
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
        assertEquals(PASSOFF_POSSIBLE_POINTS, submission.rubric().passoffTests().results().score());
        assertEquals(VerifiedStatus.ApprovedAutomatically, submission.verifiedStatus());

    }

    @Test
    void score__partialPoints() {
        Rubric phase0Rubric = constructRubric(.5f);
        Submission submission = scoreRubric(phase0Rubric);

        assertNotNull(submission);
        assertEquals(.5f, submission.score());
        assertEquals(.5f * PASSOFF_POSSIBLE_POINTS, submission.rubric().passoffTests().results().score());
        assertEquals(VerifiedStatus.ApprovedAutomatically, submission.verifiedStatus());
    }

    @Test
    void score__extraPoints() {
        Submission submission = scoreRubric(constructRubric(1.5f));

        assertNotNull(submission);
        assertEquals(1.5f, submission.score());
        assertEquals(1.5f * PASSOFF_POSSIBLE_POINTS, submission.rubric().passoffTests().results().score());
    }

    @Test
    void score__noPossiblePoints__error() {
        RubricConfig emptyRubricConfig = new RubricConfig(Phase.Phase0, null, null, null);
        setRubricConfig(Phase.Phase0, emptyRubricConfig);

        var scorer = new Scorer(gradingContext);
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
                null,
                null,
                new RubricConfig.RubricConfigItem("testCategory", "testCriteria", 30)
        );
        setRubricConfig(Phase.Quality, phase0RubricConfig);

        gradingContext = new GradingContext(
                "testNetId", Phase.Quality, "testPhasesPath", "testStagePath",
                "testRepoUrl", new File(""),
                standardCVConfig, mockObserver, false);
        addQueueItem(new QueueItem("testNetId", Phase.Phase0, Instant.now(), true));

        Rubric emptyRubric = new Rubric(null, null, null, true, "testNotes");
        Submission submission = scoreRubric(emptyRubric);

        assertNotNull(submission);
        assertTrue(submission.passed());
        assertEquals(0, submission.score());

        Mockito.verifyNoInteractions(spyCanvasIntegration);
    }

    // Helper Methods for constructing

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

        return new Rubric(
                new Rubric.RubricItem("testCategory", results, "testCriteria"),
                null,
                null,
                true,
                "testNotes"
        );
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
    private Submission scoreRubric(Rubric rubric, CommitVerificationResult commitVerification) {
        Scorer scorer = new Scorer(gradingContext);
        return scoreRubric(scorer, rubric, commitVerification);
    }
    private Submission scoreRubric(Scorer scorer, Rubric rubric, CommitVerificationResult commitVerification) {
        try {
            return scorer.score(rubric, commitVerification);
        } catch (Exception e) {
            fail("Unexpected exception thrown: ", e);
        }
        return null;
    }

    private static CommitVerificationResult constructCommitVerificationResult(boolean verified, boolean isCached) {
        String statusStr = verified ? "PASSING" : "FAILING";
        if (isCached) statusStr += "_CACHED";
        String headHash = "<" + statusStr + "_COMMIT_VERIFICATION>";

        return new CommitVerificationResult(
                verified, isCached, 0, 0, 0, 0,
                "", null, null,
                headHash, null);
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
        testProperties.setProperty("use-canvas", "false");

        ApplicationProperties.loadProperties(testProperties);
    }
}
