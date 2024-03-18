package edu.byu.cs.autograder;

import edu.byu.cs.dataAccess.DaoService;
import edu.byu.cs.model.Phase;
import edu.byu.cs.model.Rubric;
import edu.byu.cs.model.RubricConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import static edu.byu.cs.autograder.TestHelper.checkIfPassedPassoffTests;

public class PhaseFiveGrader extends PassoffTestGrader {

    private static final Logger LOGGER = LoggerFactory.getLogger(PhaseFiveGrader.class);

    private static final int MIN_UNIT_TESTS = 7;

    /**
     * Creates a new grader for phase 5
     *
     * @param netId    the netId of the student
     * @param repoUrl  the url of the student repo
     * @param observer the observer to notify of updates
     * @throws IOException if an IO error occurs
     */
    public PhaseFiveGrader(String netId, String repoUrl, Observer observer) throws IOException {
        super(null, netId, repoUrl, observer, Phase.Phase5);
    }

    @Override
    protected Set<String> getPackagesToTest() {
        return Set.of();
    }

    @Override
    protected Rubric.Results runCustomTests() {
        Set<String> excludedTests = new TestHelper().getTestFileNames(phaseTests);
        new TestHelper().compileTests(
                stageRepo,
                "server",
                new File(stageRepo, "client/src/test/java/"),
                stagePath,
                excludedTests);

        TestAnalyzer.TestNode results;
        if (!new File(stagePath, "tests").exists()) {
            results = new TestAnalyzer.TestNode();
            TestAnalyzer.TestNode.countTests(results);
        } else
            results = new TestHelper().runJUnitTests(
                    new File(stageRepo, "/client/target/client-jar-with-dependencies.jar"),
                    new File(stagePath, "tests"),
                    Set.of("clientTests"),
                    new HashSet<>());

        if (results == null) {
            results = new TestAnalyzer.TestNode();
            TestAnalyzer.TestNode.countTests(results);
            LOGGER.error("Tests failed to run for " + netId + " in phase 5");
        }

        results.testName = CUSTOM_TESTS_NAME;

        RubricConfig rubricConfig = DaoService.getRubricConfigDao().getRubricConfig(phase);

        return new Rubric.Results(getNotes(results), getUnitTestScore(results), rubricConfig.unitTests().points(), results, null);
    }

    @Override
    protected boolean passed(Rubric rubric) {
        return checkIfPassedPassoffTests(rubric);
    }
    protected float getUnitTestScore(TestAnalyzer.TestNode testResults) {
        float totalTests = testResults.numTestsFailed + testResults.numTestsPassed;

        if (totalTests == 0)
            return 0;

        if (totalTests < MIN_UNIT_TESTS)
            return (float) testResults.numTestsPassed / MIN_UNIT_TESTS;

        return testResults.numTestsPassed / totalTests;
    }

    @Override
    protected String getNotes(TestAnalyzer.TestNode testResults) {
        if (testResults.numTestsPassed + testResults.numTestsFailed < MIN_UNIT_TESTS)
            return "Not enough tests: each service method should have a positive and negative test";

        return switch (testResults.numTestsFailed) {
            case 0 -> "All tests passed";
            case 1 -> "1 test failed";
            default -> testResults.numTestsFailed + " tests failed";
        };
    }

    @Override
    protected String getCanvasRubricId(Rubric.RubricType type) {
        return switch (type) {
            case PASSOFF_TESTS -> "_5202";
            case UNIT_TESTS -> "90344_776";
            case QUALITY -> "_3003";
        };
    }
}
