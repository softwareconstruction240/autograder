package edu.byu.cs.autograder.score.latepenalties;

import edu.byu.cs.autograder.GradingContext;
import edu.byu.cs.dataAccess.DaoService;
import edu.byu.cs.dataAccess.DataAccessException;
import edu.byu.cs.dataAccess.daoInterface.ConfigurationDao;
import edu.byu.cs.dataAccess.daoInterface.RubricConfigDao;
import edu.byu.cs.model.Phase;
import edu.byu.cs.model.RubricConfig;
import edu.byu.cs.model.Rubric;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;

import java.io.File;
import java.util.ArrayList;
import java.util.EnumMap;

public abstract class LatePenaltyCalculatorTest {

    static protected RubricConfigDao rubricConfigDao;
    static protected ArrayList<RubricConfig> rubricConfigs;
    static protected LatePenaltyCalculator latePenaltyCalculator;
    static protected GradingContext gradingContext;
    static protected Rubric testRubricOneItem;
    static protected Rubric testRubricTwoItems;
    static protected Rubric testRubricThreeItems;

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
    }

    @Test
    abstract void testEarlySubmission() throws DataAccessException;

    @Test
    abstract void testOnTimeSubmission() throws DataAccessException;

    @Test
    abstract void testOneDayLate() throws DataAccessException;

    @Test
    abstract void testMaxLate() throws DataAccessException;

    @Test
    abstract public void testPenaltyConfigOverride() throws DataAccessException;

    @Test
    abstract void testLatePenaltyNotesFormat() throws DataAccessException;
}
