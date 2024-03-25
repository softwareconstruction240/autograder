package edu.byu.cs.autograder.test;

import edu.byu.cs.autograder.GradingContext;
import edu.byu.cs.autograder.GradingException;
import edu.byu.cs.model.Phase;
import edu.byu.cs.model.RubricConfig;
import edu.byu.cs.util.PhaseUtils;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

public class UnitTestGrader extends TestGrader {
    public UnitTestGrader(GradingContext gradingContext) {
        super(gradingContext);
    }

    @Override
    protected String name() {
        return "unit";
    }

    @Override
    protected Set<String> excludedTests() throws GradingException {
        return testHelper.getTestFileNames(phaseTests);
    }

    @Override
    protected File testsToCompile() {
        return new File(gradingContext.stageRepo(), module + "/src/test/java/");
    }

    @Override
    protected Set<String> packagesToTest() throws GradingException {
        return PhaseUtils.unitTestPackagesToTest(gradingContext.phase());
    }

    @Override
    protected Set<String> extraCreditTests() {
        return new HashSet<>();
    }

    @Override
    protected String testName() {
        return "Custom Tests";
    }

    @Override
    protected float getScore(TestAnalyzer.TestNode testResults) throws GradingException {
        float totalTests = testResults.numTestsFailed + testResults.numTestsPassed;

        if (totalTests == 0) return 0;

        int minTests = PhaseUtils.minUnitTests(gradingContext.phase());

        if (totalTests < minTests) return (float) testResults.numTestsPassed / minTests;

        return testResults.numTestsPassed / totalTests;
    }

    @Override
    protected String getNotes(TestAnalyzer.TestNode testResults) throws GradingException {
        if (testResults.numTestsPassed + testResults.numTestsFailed < PhaseUtils.minUnitTests(gradingContext.phase()))
            return "Not enough tests: each " + PhaseUtils.unitTestCodeUnderTest(gradingContext.phase()) +
                    " method should have a positive and negative test";

        return switch (testResults.numTestsFailed) {
            case 0 -> "All tests passed";
            case 1 -> "1 test failed";
            default -> testResults.numTestsFailed + " tests failed";
        };
    }

    @Override
    protected RubricConfig.RubricConfigItem rubricConfigItem(RubricConfig config) {
        return config.unitTests();
    }

}
