package edu.byu.cs.autograder.test;

import edu.byu.cs.autograder.GradingContext;
import edu.byu.cs.autograder.GradingException;
import edu.byu.cs.model.ClassCoverageAnalysis;
import edu.byu.cs.model.Rubric;
import edu.byu.cs.model.TestOutput;
import edu.byu.cs.model.TestNode;
import edu.byu.cs.util.PhaseUtils;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

public class UnitTestGrader extends TestGrader {
    private static final float MIN_COVERAGE = 0.7f;

    private Float coverageProportion = null;

    public UnitTestGrader(GradingContext gradingContext) {
        super(gradingContext);
    }

    @Override
    protected String name() {
        return "unit";
    }

    @Override
    protected Set<File> testsToCompile() {
        return Set.of(new File(gradingContext.stageRepo(), module + "/src/test/java/"));
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
    protected float getScore(TestOutput testOutput) throws GradingException {
        float totalTests = testOutput.root().getNumTestsFailed() + testOutput.root().getNumTestsPassed();
        if (totalTests == 0) return 0;
        float testPassingProportion = testOutput.root().getNumTestsPassed() / totalTests;
        return (coverageProportion(testOutput) / MIN_COVERAGE) * testPassingProportion;
    }

    @Override
    protected String getNotes(TestOutput testOutput) throws GradingException {
        TestNode testResults = testOutput.root();
        if (testResults.getNumTestsPassed() + testResults.getNumTestsFailed() < PhaseUtils.minUnitTests(gradingContext.phase()))
            return "Not enough tests: each " + PhaseUtils.unitTestCodeUnderTest(gradingContext.phase()) +
                    " method should have a positive and negative test";

        String numPassingOutput = switch (testResults.getNumTestsFailed()) {
            case 0 -> "All tests passed";
            case 1 -> "1 test failed";
            default -> testResults.getNumTestsFailed() + " tests failed";
        };

        return String.format("%s\nCoverage: %.2f%% / %.0f%%",
                numPassingOutput, coverageProportion(testOutput) * 100, MIN_COVERAGE * 100);
    }

    @Override
    protected Rubric.RubricType rubricType() {
        return Rubric.RubricType.UNIT_TESTS;
    }

    private float coverageProportion(TestOutput testOutput) {
        if(coverageProportion != null) {
            return coverageProportion;
        }

        int totalCovered = 0;
        int totalMissed = 0;
        for (ClassCoverageAnalysis analysis : testOutput.coverage().classAnalyses()) {
            totalCovered += analysis.covered();
            totalMissed += analysis.missed();
        }

        float total = totalCovered + totalMissed;
        if(total == 0) {
            return 0;
        }
        coverageProportion = totalCovered / total;
        return coverageProportion;
    }
}
