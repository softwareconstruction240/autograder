package edu.byu.cs.autograder.score.latepenalties;

import edu.byu.cs.autograder.GradingContext;
import edu.byu.cs.autograder.GradingException;
import edu.byu.cs.autograder.git.CommitVerificationReport;
import edu.byu.cs.autograder.git.CommitVerificationResult;
import edu.byu.cs.autograder.score.penalties.PenaltyCalculator;
import edu.byu.cs.dataAccess.DaoService;
import edu.byu.cs.dataAccess.DataAccessException;
import edu.byu.cs.dataAccess.daoInterface.ConfigurationDao;
import edu.byu.cs.dataAccess.daoInterface.RubricConfigDao;
import edu.byu.cs.model.Phase;
import edu.byu.cs.model.QueueItem;
import edu.byu.cs.model.RubricConfig;
import edu.byu.cs.model.Rubric;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

public abstract class PenaltyCalculatorTest {

    static protected PenaltyCalculator latePenaltyCalculator;
    static protected RubricConfigDao rubricConfigDao;
    static protected ArrayList<RubricConfig> rubricConfigs;
    static protected GradingContext gradingContext;
    static protected Rubric testRubricOneItem;
    static protected Rubric testRubricTwoItems;
    static protected Rubric testRubricThreeItems;
    static protected CommitVerificationReport mockCommitReport;

    protected static void setUp() throws DataAccessException {
        //dao init
        DaoService.initializeMemoryDAOs();
        rubricConfigDao = DaoService.getRubricConfigDao();

        //config values init
        DaoService.getConfigurationDao().setConfiguration(ConfigurationDao.Configuration.PER_DAY_LATE_PENALTY, 0.1f, Float.class);
        DaoService.getConfigurationDao().setConfiguration(ConfigurationDao.Configuration.MAX_LATE_DAYS_TO_PENALIZE, 5, Integer.class);

        rubricConfigs = new ArrayList<>();
        for (Phase phase : Phase.values()) {
            rubricConfigs.add(rubricConfigDao.getRubricConfig(phase));
        }

        gradingContext = new GradingContext(
                "testNetId", Phase.Phase0, "testPhasesPath", "testStagePath",
                "testRepoUrl", new File(""),
                null, null, false
        );

        // rubric init
        EnumMap<Rubric.RubricType, Rubric.RubricItem> items = new EnumMap<>(Rubric.RubricType.class);
        Rubric.Results results = new Rubric.Results("notes", 10.0f, 10, null, "textResults");
        Rubric.RubricItem rubricItem = new Rubric.RubricItem("testCategory", results, "testCriteria");
        items.put(Rubric.RubricType.PASSOFF_TESTS, rubricItem);
        testRubricOneItem = new Rubric(items, true, "");
        items.put(Rubric.RubricType.QUALITY, rubricItem);
        testRubricTwoItems = new Rubric(items, true, "");
        items.put(Rubric.RubricType.UNIT_TESTS, rubricItem);
        testRubricThreeItems = new Rubric(items, true, "");

        mockCommitReport = new CommitVerificationReport(null, new CommitVerificationResult(
                true,
                true,
                100,
                100,
                10,
                false,
                0,
                "",
                null,
                null,
                null,
                "",
                null)
        );


        //add queue item so that the hand-in date of the submission can be resolved.
        DaoService.getQueueDao().add(new QueueItem("testNetId", Phase.Phase4, Instant.now(), true));
    }

    @ParameterizedTest
    @MethodSource("getRubrics")
    abstract void testEarlySubmission(Rubric testRubric) throws DataAccessException, GradingException;

    @ParameterizedTest
    @MethodSource("getRubrics")
    abstract void testOnTimeSubmission(Rubric testRubric) throws DataAccessException, GradingException;

    @ParameterizedTest
    @MethodSource("getRubrics")
    abstract void testOneDayLate(Rubric testRubric) throws DataAccessException, GradingException;

    @ParameterizedTest
    @MethodSource("getRubrics")
    abstract void testMaxLate(Rubric testRubric) throws DataAccessException, GradingException;

    @ParameterizedTest
    @MethodSource("getRubrics")
    abstract public void testPenaltyConfigOverride(Rubric testRubric) throws DataAccessException, GradingException;

    @ParameterizedTest
    @MethodSource("getRubrics")
    abstract void testLatePenaltyNotesFormat(Rubric testRubric) throws DataAccessException, GradingException;

    protected static Iterable<? extends Arguments> getRubrics(){
        return List.of(Arguments.of(testRubricOneItem), Arguments.of(testRubricTwoItems), Arguments.of(testRubricThreeItems));
    }
}
