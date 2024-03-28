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

        gradingContext = new GradingContext("testNetId", Phase.Phase0, "testPhasesPath", "testStagePath", "testRepoUrl", null, 10, mockObserver, false);


    }

    @Test
    void score() {
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

        Rubric phase0Rubric = getRubric(1f);
        Scorer scorer = new Scorer(gradingContext);

        Submission submission = null;
        try {
            submission = scorer.score(phase0Rubric, 0);
        } catch (GradingException e) {
            fail("Unexpected exception thrown: ", e);
        }

        assertNotNull(submission);
        assertEquals(PASSOFF_POSSIBLE_POINTS, submission.score());
    }

    @Test
    void getScore__fullPoints() {
        Scorer scorer = new Scorer(gradingContext);
        Float score = null;
        Rubric phase0Rubric = getRubric(1f);
        try {
            score = scorer.getScore(phase0Rubric);
        } catch (GradingException e) {
            fail("Unexpected exception thrown: ", e);
        }

        assertNotNull(score);
        assertEquals(1f, score);
    }

    @Test
    void getScore__partialPoints() {
        Rubric phase0Rubric = getRubric(.5f);

        Scorer scorer = new Scorer(gradingContext);
        Float score = null;
        try {
            score = scorer.getScore(phase0Rubric);
        } catch (GradingException e) {
            fail("Unexpected exception thrown: ", e);
        }

        assertNotNull(score);
        assertEquals(.5f, score);
    }

    @Test
    void getScore__extraPoints() {
        Rubric phase0Rubric = getRubric(1.5f);

        Scorer scorer = new Scorer(gradingContext);
        Float score = null;
        try {
            score = scorer.getScore(phase0Rubric);
        } catch (GradingException e) {
            fail("Unexpected exception thrown: ", e);
        }

        assertNotNull(score);
        assertEquals(1.5f, score);
    }

    @Test
    void getScore__noPossiblePoints() {
        RubricConfig emptyRubricConfig = new RubricConfig(Phase.Phase0, null, null, null);
        DaoService.getRubricConfigDao().setRubricConfig(Phase.Phase0, emptyRubricConfig);

        assertThrows(GradingException.class, () -> {
            new Scorer(gradingContext).getScore(getRubric(1f));
        });
    }

    /**
     * Helper method to create a Rubric object with the given expected percent, based on PASSOFF_POSSIBLE_POINTS
     *
     * @param expectedPercent the expected score (0-1)
     * @return the Rubric object
     */
    private Rubric getRubric(float expectedPercent) {
        Rubric.Results results = new Rubric.Results(
                "testNotes",
                expectedPercent * PASSOFF_POSSIBLE_POINTS,
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