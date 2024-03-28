package edu.byu.cs.autograder.score;

import edu.byu.cs.autograder.GradingContext;
import edu.byu.cs.autograder.GradingException;
import edu.byu.cs.dataAccess.DaoService;
import edu.byu.cs.dataAccess.RubricConfigDao;
import edu.byu.cs.dataAccess.memory.QueueMemoryDao;
import edu.byu.cs.dataAccess.memory.RubricConfigMemoryDao;
import edu.byu.cs.dataAccess.memory.SubmissionMemoryDao;
import edu.byu.cs.dataAccess.memory.UserMemoryDao;
import edu.byu.cs.model.Phase;
import edu.byu.cs.model.Rubric;
import edu.byu.cs.model.RubricConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ScorerTest {
    GradingContext gradingContext = new GradingContext("testNetId", Phase.Phase0, "testPhasesPath", "testStagePath", "testRepoUrl", null, 10, null, false);

    private static final int PASSOFF_POSSIBLE_POINTS = 10;

    @BeforeEach
    void setUp() {
        DaoService.setRubricConfigDao(new RubricConfigMemoryDao());
        DaoService.setUserDao(new UserMemoryDao());
        DaoService.setQueueDao(new QueueMemoryDao());
        DaoService.setSubmissionDao(new SubmissionMemoryDao());

        RubricConfigDao rubricConfigDao = DaoService.getRubricConfigDao();

        RubricConfig phase0RubricConfig = new RubricConfig(
                Phase.Phase0,
                new RubricConfig.RubricConfigItem("testCategory", "testCriteria", PASSOFF_POSSIBLE_POINTS),
                null,
                null
        );
        rubricConfigDao.setRubricConfig(Phase.Phase0, phase0RubricConfig);
    }

    @Test
    void score() {
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
}