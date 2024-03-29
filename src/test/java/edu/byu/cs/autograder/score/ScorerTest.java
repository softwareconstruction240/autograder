package edu.byu.cs.autograder.score;

import edu.byu.cs.autograder.Grader;
import edu.byu.cs.autograder.GradingContext;
import edu.byu.cs.autograder.GradingException;
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

        gradingContext = new GradingContext("testNetId", Phase.Phase0, "testPhasesPath", "testStagePath", "testRepoUrl", new File(""), 10, mockObserver, false);


    }

    @Test
    void score__fullPoints() {
        Rubric phase0Rubric = getRubric(1f);
        Scorer scorer = new Scorer(gradingContext);

        Submission submission = null;
        try {
            submission = scorer.score(phase0Rubric, 0);
        } catch (GradingException e) {
            fail("Unexpected exception thrown: ", e);
        }

        assertNotNull(submission);
        assertEquals(1, submission.score());
        assertEquals(PASSOFF_POSSIBLE_POINTS, submission.rubric().passoffTests().results().score());
    }

    @Test
    void score__partialPoints() {
        Rubric phase0Rubric = getRubric(.5f);
        Scorer scorer = new Scorer(gradingContext);

        Submission submission = null;
        try {
            submission = scorer.score(phase0Rubric, 0);
        } catch (GradingException e) {
            fail("Unexpected exception thrown: ", e);
        }

        assertNotNull(submission);
        assertEquals(.5f, submission.score());
        assertEquals(.5f * PASSOFF_POSSIBLE_POINTS, submission.rubric().passoffTests().results().score());
    }

    @Test
    void score__extraPoints() {
        Rubric phase0Rubric = getRubric(1.5f);
        Scorer scorer = new Scorer(gradingContext);

        Submission submission = null;
        try {
            submission = scorer.score(phase0Rubric, 0);
        } catch (GradingException e) {
            fail("Unexpected exception thrown: ", e);
        }

        assertNotNull(submission);
        assertEquals(1.5f, submission.score());
        assertEquals(1.5f * PASSOFF_POSSIBLE_POINTS, submission.rubric().passoffTests().results().score());
    }

    @Test
    void score__noPossiblePoints__error() {
        RubricConfig emptyRubricConfig = new RubricConfig(Phase.Phase0, null, null, null);
        DaoService.getRubricConfigDao().setRubricConfig(Phase.Phase0, emptyRubricConfig);

        Scorer scorer = new Scorer(gradingContext);
        assertThrows(GradingException.class, () -> scorer.score(getRubric(1f), 0));
    }

    @Test
    void score__adminSubmission() {
        gradingContext = new GradingContext("testNetId", Phase.Phase0, "testPhasesPath", "testStagePath", "testRepoUrl", new File(""), 10, mockObserver, true);
        Scorer scorer = new Scorer(gradingContext);

        Submission submission = null;
        try {
            submission = scorer.score(getRubric(1f), 0);
        } catch (GradingException e) {
            fail("Unexpected exception thrown: ", e);
        }

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

        gradingContext = new GradingContext("testNetId", Phase.Quality, "testPhasesPath", "testStagePath", "testRepoUrl", new File(""), 10, mockObserver, false);
        DaoService.getQueueDao().add(new QueueItem("testNetId", Phase.Phase0, Instant.now(), true));
        Scorer scorer = new Scorer(gradingContext);

        Submission submission = null;
        try {
            Rubric emptyRubric = new Rubric(null, null, null, true, "testNotes");
            submission = scorer.score(emptyRubric, 0);
        } catch (GradingException e) {
            fail("Unexpected exception thrown: ", e);
        }

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
    private Rubric getRubric(float score) {
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