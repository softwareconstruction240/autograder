package edu.byu.cs.autograder.test;

import edu.byu.cs.autograder.GradingContext;
import edu.byu.cs.autograder.GradingException;
import edu.byu.cs.model.Rubric;
import edu.byu.cs.model.TestOutput;
import edu.byu.cs.model.TestNode;
import edu.byu.cs.util.PhaseUtils;

import java.io.File;
import java.util.*;

/**
 * Runs and scores the passoff tests for the current phase a submission is graded for
 */
public class PassoffTestGrader extends TestGrader {

    public PassoffTestGrader(GradingContext gradingContext) {
        super(gradingContext);
    }

    @Override
    protected String name() {
        return "passoff";
    }

    @Override
    protected Set<File> testsToCompile() {
        return Set.of(phaseTests);
    }

    @Override
    protected Set<String> packagesToTest() throws GradingException {
        return PhaseUtils.passoffPackagesToTest(gradingContext.phase());
    }

    @Override
    protected String testName() {
        return "Passoff Tests";
    }

    @Override
    protected float getScore(TestOutput testOutput) {
        TestNode testResults = testOutput.root();
        float totalStandardTests = testResults.getNumTestsFailed() + testResults.getNumTestsPassed();

        if (totalStandardTests == 0) return 0;

        return testResults.getNumTestsPassed() / totalStandardTests;
    }

    @Override
    protected String getNotes(TestOutput testOutput) {
        TestNode testResults = testOutput.root();
        StringBuilder notes = new StringBuilder();

        if (testResults == null) return "No tests were run";

        Integer totalRequiredTests = testResults.getNumTestsPassed() + testResults.getNumTestsFailed();
        if (testResults.getNumTestsFailed() == 0) {
            notes.append(testResults.getNumTestsPassed() + "/"+ totalRequiredTests + " required tests passed");
        }
        else {
            notes.append(testResults.getNumTestsFailed() + "/" + totalRequiredTests + " required tests failed");
        }

        return notes.toString();
    }

    @Override
    protected Rubric.RubricType rubricType() {
        return Rubric.RubricType.PASSOFF_TESTS;
    }

    @Override
    protected Set<String> modulesToCheckCoverage() {
        return Set.of();
    }
}
