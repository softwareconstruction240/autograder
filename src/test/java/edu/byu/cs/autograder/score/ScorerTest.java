package edu.byu.cs.autograder.score;

import edu.byu.cs.autograder.Grader;
import edu.byu.cs.autograder.GradingContext;
import edu.byu.cs.autograder.GradingException;
import edu.byu.cs.autograder.git.CommitVerificationResult;
import edu.byu.cs.canvas.CanvasException;
import edu.byu.cs.canvas.CanvasIntegration;
import edu.byu.cs.canvas.CanvasService;
import edu.byu.cs.canvas.FakeCanvasIntegration;
import edu.byu.cs.dataAccess.DaoService;
import edu.byu.cs.dataAccess.memory.QueueMemoryDao;
import edu.byu.cs.dataAccess.memory.RubricConfigMemoryDao;
import edu.byu.cs.dataAccess.memory.SubmissionMemoryDao;
import edu.byu.cs.dataAccess.memory.UserMemoryDao;
import edu.byu.cs.model.*;
import edu.byu.cs.properties.ApplicationProperties;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.File;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class ScorerTest {

    Grader.Observer mockObserver = null;
    CanvasIntegration spyCanvasIntegration = null;
    GradingContext gradingContext = null;

    private static final int PASSOFF_POSSIBLE_POINTS = 10;

    private static final CommitVerificationResult PASSING_COMMIT_VERIFICATION = new CommitVerificationResult(
            true, 0, 0,
            "", null, null,
            "<PASSING_COMMIT_VERIFICATION>", null);
    private static final CommitVerificationResult FAILING_COMMIT_VERIFICATION = new CommitVerificationResult(
            false, 0, 0,
            "Failing verification", null, null,
            "<FAILING_COMMIT_VERIFICATION>", null);


    @BeforeAll
    static void setUpAll() {
        loadApplicationProperties();
    }

    @BeforeEach
    void setUp() {
        spyCanvasIntegration = Mockito.spy(new FakeCanvasIntegration());
        CanvasService.setCanvasIntegration(spyCanvasIntegration);

        try {
            when(spyCanvasIntegration.getAssignmentDueDateForStudent(Mockito.anyInt(), Mockito.anyInt())).thenReturn(
                    ZonedDateTime.now().plusDays(1)
            );

            when(spyCanvasIntegration.getSubmission(Mockito.anyInt(), Mockito.anyInt())).thenReturn(
                    new CanvasIntegration.CanvasSubmission("testUrl", new CanvasIntegration.RubricAssessment(new HashMap<>()), 1f)
            );
        } catch (CanvasException e) {
            fail("Unexpected exception thrown: ", e);
        }

        DaoService.setRubricConfigDao(new RubricConfigMemoryDao());
        DaoService.setUserDao(new UserMemoryDao());
        DaoService.setQueueDao(new QueueMemoryDao());
        DaoService.setSubmissionDao(new SubmissionMemoryDao());

        RubricConfig phase0RubricConfig = new RubricConfig(
                Phase.Phase0,
                new RubricConfig.RubricConfigItem("testCategory", "testCriteria", PASSOFF_POSSIBLE_POINTS),
                null,
                null
        );
        DaoService.getRubricConfigDao().setRubricConfig(Phase.Phase0, phase0RubricConfig);
        DaoService.getUserDao().insertUser(new User("testNetId", 123, "testFirst", "testLast", "testRepoUrl", User.Role.STUDENT));
        DaoService.getQueueDao().add(new QueueItem("testNetId", Phase.Phase0, Instant.now(), false));

        mockObserver = Mockito.mock(Grader.Observer.class);

        gradingContext = new GradingContext(
                "testNetId", Phase.Phase0, "testPhasesPath", "testStagePath",
                "testRepoUrl", new File(""),
                10, 3, 10,
                mockObserver, false);


    }

    @Test
    void score__fullPoints() {
        Rubric phase0Rubric = constructRubric(1f);
        Submission submission = scoreRubric(phase0Rubric);

        assertNotNull(submission);
        assertEquals(1, submission.score());
        assertEquals(PASSOFF_POSSIBLE_POINTS, submission.rubric().passoffTests().results().score());
        assertEquals(Submission.VerifiedStatus.ApprovedAutomatically, submission.verifiedStatus());

    }

    @Test
    void score__partialPoints() {
        Rubric phase0Rubric = constructRubric(.5f);
        Submission submission = scoreRubric(phase0Rubric);

        assertNotNull(submission);
        assertEquals(.5f, submission.score());
        assertEquals(.5f * PASSOFF_POSSIBLE_POINTS, submission.rubric().passoffTests().results().score());
        assertEquals(Submission.VerifiedStatus.ApprovedAutomatically, submission.verifiedStatus());
    }

    @Test
    void score__extraPoints() {
        Rubric phase0Rubric = constructRubric(1.5f);
        Submission submission = scoreRubric(phase0Rubric);

        assertNotNull(submission);
        assertEquals(1.5f, submission.score());
        assertEquals(1.5f * PASSOFF_POSSIBLE_POINTS, submission.rubric().passoffTests().results().score());
    }

    @Test
    void score__noPossiblePoints__error() {
        RubricConfig emptyRubricConfig = new RubricConfig(Phase.Phase0, null, null, null);
        DaoService.getRubricConfigDao().setRubricConfig(Phase.Phase0, emptyRubricConfig);

        assertThrows(GradingException.class, () -> scoreRubric(constructRubric(1f)));
    }

    @Test
    void score__commitVerification__notVerified() {
        Rubric phase0Rubric = constructRubric(1.0f);

        Submission submission = scoreRubric(phase0Rubric, FAILING_COMMIT_VERIFICATION);

        assertNotNull(submission);
        assertEquals(1.0f, submission.score());
        assertTrue(submission.passed());
        assertEquals(Submission.VerifiedStatus.Unapproved, submission.verifiedStatus());
        assertNull(submission.verification());

        Mockito.verifyNoInteractions(spyCanvasIntegration);
    }

    @Test
    void score__adminSubmission() {
        gradingContext = new GradingContext(
                "testNetId", Phase.Phase0, "testPhasesPath", "testStagePath",
                "testRepoUrl", new File(""),
                10, 3, 10,
                mockObserver, true);

        Submission submission = scoreRubric(constructRubric(1f));

        assertNotNull(submission);
        assertTrue(submission.admin());

        Mockito.verifyNoInteractions(spyCanvasIntegration);
    }

    @Test
    void score__phaseNotGradeable() {
        RubricConfig phase0RubricConfig = new RubricConfig(
                Phase.Quality,
                null,
                null,
                new RubricConfig.RubricConfigItem("testCategory", "testCriteria", 30)
        );
        DaoService.getRubricConfigDao().setRubricConfig(Phase.Quality, phase0RubricConfig);

        gradingContext = new GradingContext(
                "testNetId", Phase.Quality, "testPhasesPath", "testStagePath",
                "testRepoUrl", new File(""),
                10, 3, 10,
                mockObserver, false);
        DaoService.getQueueDao().add(new QueueItem("testNetId", Phase.Phase0, Instant.now(), true));

        Rubric emptyRubric = new Rubric(null, null, null, true, "testNotes");
        Submission submission = scoreRubric(emptyRubric);

        assertNotNull(submission);
        assertTrue(submission.passed());
        assertEquals(0, submission.score());

        Mockito.verifyNoInteractions(spyCanvasIntegration);
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

        return new Rubric(
                new Rubric.RubricItem("testCategory", results, "testCriteria"),
                null,
                null,
                true,
                "testNotes"
        );
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
        } catch (GradingException e) {
            fail("Unexpected exception thrown: ", e);
        }
        return null;
    }

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
