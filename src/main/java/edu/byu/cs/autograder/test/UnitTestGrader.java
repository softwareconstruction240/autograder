package edu.byu.cs.autograder.test;

import java.io.File;
import java.util.Set;

import edu.byu.cs.autograder.GradingContext;
import edu.byu.cs.autograder.GradingException;
import edu.byu.cs.model.ClassCoverageAnalysis;
import edu.byu.cs.model.CoverageAnalysis;
import edu.byu.cs.model.Rubric;
import edu.byu.cs.model.TestNode;
import edu.byu.cs.model.TestOutput;
import edu.byu.cs.util.PhaseUtils;

/**
 * Runs and scores the unit tests for the phase a submission is graded for
 */
public class UnitTestGrader extends TestGrader {

    private final float targetPercent = 0.8F; // how much we want covered, change me if too low or high
    private final float extraCreditPercent = 0.9F; // double check with professors

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
    protected String testName() {
        return "Custom Tests";
    }

    @Override
    protected float getScore(TestOutput testOutput) throws GradingException {
        TestNode testResults = testOutput.root();
        float totalTests = testResults.getNumTestsFailed() + testResults.getNumTestsPassed();

        if (totalTests == 0) return 0;

        if (testResults.getNumTestsFailed() > 0) {
            return 0;
        }

        float coveragePercent = getCoveragePercent(testOutput.coverage());
        if (Float.isNaN(coveragePercent)){
            return 0;
        }

        if (coveragePercent > extraCreditPercent) {
            return 1.05F;
        }
        if (coveragePercent > targetPercent){
            return 1;
        }

        return coveragePercent / targetPercent;
    }

    @Override
    protected String getNotes(TestOutput testOutput) {
        TestNode testResults = testOutput.root();
        StringBuilder notes = new StringBuilder();

        float totalTests = testResults.getNumTestsFailed() + testResults.getNumTestsPassed();
        if (totalTests == 0) {
            notes.append("Did not find any unit tests. See Rubric for unit test requirement details")
                    .append("\n");
            return notes.toString();
        }

        if (testResults.getNumTestsFailed() > 0) {
            notes.append(testResults.getNumTestsFailed() + " unit tests failed")
                    .append("\n")
                    .append("See Details for failed test")
                    .append("\n");
            return notes.toString();
        }

        float coverage = getCoveragePercent(testOutput.coverage());
        if (Float.isNaN(coverage)){
            notes.append("Could not calculate Coverage Percent. See Rubric for unit test requirement details")
                .append("\n");
            return notes.toString();
        }

        notes.append("Coverage: " + coverage*100 + "%")
            .append("\n");


        for (ClassCoverageAnalysis i : testOutput.coverage().classAnalyses()){
            var total = (i.covered() + i.missed());
            boolean isGoodCoverage = ((i.covered() * 1.0) / total) > targetPercent;
            notes.append((isGoodCoverage) ? "✓" : "✗").append(" ")
                    .append(i.packageName())
                    .append(".")
                    .append(i.className())
                    .append("\n");
        }
        notes.append("See Details for line coverage count")
                .append("\n");
        return notes.toString();
    }

    private float getCoveragePercent(CoverageAnalysis coverage) {
        float covered = 0;
        float total = 0;
        for (ClassCoverageAnalysis i : coverage.classAnalyses()){
            covered += i.covered();
            total += (i.covered() + i.missed());
        }
        return covered / total;
    }

    @Override
    protected Rubric.RubricType rubricType() {
        return Rubric.RubricType.UNIT_TESTS;
    }

    @Override
    protected Set<String> modulesToCheckCoverage() throws GradingException {
        return PhaseUtils.unitTestModulesToCheckCoverage(gradingContext.phase());
    }
}
