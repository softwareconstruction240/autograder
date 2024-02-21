package edu.byu.cs.autograder;

import edu.byu.cs.dataAccess.DaoService;
import edu.byu.cs.model.Phase;
import edu.byu.cs.model.Rubric;
import edu.byu.cs.model.RubricConfig;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class PhaseThreeGrader extends PassoffTestGrader {
    /**
     * Creates a new grader for phase 3
     *
     * @param netId    the netId of the student
     * @param repoUrl  the url of the student repo
     * @param observer the observer to notify of updates
     * @throws IOException if an IO error occurs
     */
    public PhaseThreeGrader(String netId, String repoUrl, Observer observer) throws IOException {
        super("./phases/phase3", netId, repoUrl, observer, Phase.Phase3);
    }

    @Override
    protected Rubric.Results runCustomTests() {
        Set<String> excludedTests = new TestHelper().getTestFileNames(phaseTests);
        new TestHelper().compileTests(
                stageRepo,
                "server",
                new File(stageRepo, "server/src/test/java"),
                stagePath,
                excludedTests);

        TestAnalyzer.TestNode results = new TestHelper().runJUnitTests(
                new File(stageRepo, "/server/target/server-jar-with-dependencies.jar"),
                new File(stagePath, "tests"),
                new HashSet<>()

        );

        results.testName = CUSTOM_TESTS_NAME;

        RubricConfig rubricConfig = DaoService.getRubricConfigDao().getRubricConfig(phase);

        return new Rubric.Results(getNotes(results), getUnitTestScore(results), rubricConfig.unitTests().points(), results, null);
    }

    @Override
    protected boolean passed(Rubric rubric) {
        boolean passed = true;

        if (rubric.passoffTests() != null && rubric.passoffTests().results() != null)
            if (rubric.passoffTests().results().score() < rubric.passoffTests().results().possiblePoints())
                passed = false;

        if (rubric.unitTests() != null && rubric.unitTests().results() != null)
            if (rubric.unitTests().results().score() < rubric.unitTests().results().possiblePoints())
                passed = false;

        // TODO: enable quality check
//        if (rubric.quality() != null && rubric.quality().results() != null) {
//            if (rubric.quality().results().score() < 1)
//                passed = false;
//        }

        return passed;
    }
    protected float getUnitTestScore(TestAnalyzer.TestNode testResults) {
        float totalTests = testResults.numTestsFailed + testResults.numTestsPassed;

        if (totalTests == 0)
            throw new RuntimeException("No standard tests found in the test results");

        return testResults.numTestsPassed / totalTests;
    }

    protected String getNotes(TestAnalyzer.TestNode testResults) {
        return switch (testResults.numTestsFailed) {
            case 0 -> "All tests passed";
            case 1 -> "1 test failed. All tests must pass";
            default -> testResults.numTestsFailed + " tests failed. All tests must pass";
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
