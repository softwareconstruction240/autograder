package edu.byu.cs.autograder.test;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

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
        TestNode testResults = testOutput.root();
        float totalTests = testResults.getNumTestsFailed() + testResults.getNumTestsPassed();

        if (totalTests == 0) return 0;

        if (testResults.getNumTestsFailed() > 0) {
            return 0;
        }

        float coveragePercent = getCoveragePercent(testOutput.coverage());
        float targetPercent = 0.8F; // how much we want covered, change me if too low or high

        if (coveragePercent > targetPercent){
            return 1;
        }

        return coveragePercent / targetPercent;
    }

    @Override
    protected String getNotes(TestOutput testOutput) throws GradingException {
        TestNode testResults = testOutput.root();
        float coverage = getCoveragePercent(testOutput.coverage());
        StringBuilder notes = new StringBuilder("Coverage: " + coverage*100);
        notes.append("\n");
        var pattern = Pattern.compile("^" + PhaseUtils.unitTestPackageForCoverage(gradingContext.phase()));
        for (ClassCoverageAnalysis i : testOutput.coverage().classAnalyses()){
            var matcher = pattern.matcher(i.packageName());
            //TODO: make sure the class is in the covered package, probably include a phase util for this
            if (matcher.find()){
                var total = (i.covered() + i.missed());
                notes.append(i.packageName())
                        .append(".")
                        .append(i.className())
                        .append(":")
                        .append(i.covered())
                        .append("/")
                        .append(total)
                        .append("\n");
            }
        }
        return notes.toString();
        
    }

    private float getCoveragePercent(CoverageAnalysis coverage) throws GradingException{
        float covered = 0;
        float total = 0;
        var pattern = Pattern.compile("^" + PhaseUtils.unitTestPackageForCoverage(gradingContext.phase()));
        for (ClassCoverageAnalysis i : coverage.classAnalyses()){
            var matcher = pattern.matcher(i.packageName());
            //TODO: make sure the class is in the covered package, probably include a phase util for this
            if (matcher.find()){
                covered += i.covered();
                total += (i.covered() + i.missed());
            }
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
