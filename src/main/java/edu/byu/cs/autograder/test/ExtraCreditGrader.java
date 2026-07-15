package edu.byu.cs.autograder.test;

import edu.byu.cs.autograder.GradingContext;
import edu.byu.cs.autograder.GradingException;
import edu.byu.cs.model.Rubric;
import edu.byu.cs.model.TestNode;
import edu.byu.cs.model.TestOutput;
import edu.byu.cs.util.PhaseUtils;

import java.io.File;
import java.util.*;

/**
 * Runs and scores the extra credit tests for the phase a submission is graded for
 */
public class ExtraCreditGrader extends TestGrader {
    public ExtraCreditGrader(GradingContext gradingContext) {
        super(gradingContext);
    }

    @Override
    protected String name() {
        return "extra credit";
    }

    @Override
    protected Set<File> testsToCompile() {
        return Set.of(phaseTests);
    }

    @Override
    protected Set<String> packagesToTest() throws GradingException {
        return PhaseUtils.extraCreditPackagesToTest(gradingContext.phase());
    }

    @Override
    protected String testName() {
        return "Extra Credit";
    }

    @Override
    protected float getScore(TestOutput testOutput) {
        TestNode testResults = testOutput.root();
        float score = 0;

        for (TestNode child : testResults.getChildren().values()) {
            score += (float) Math.floor((float) child.getNumTestsPassed() /
                    (child.getNumTestsPassed() + child.getNumTestsFailed()));
        }

        return score / testResults.getChildren().size();
    }

    @Override
    protected String getNotes(TestOutput testOutput) {
        if (testOutput.root() == null) return "No tests were run";

        return "Extra credit tests: +" + (getScore(testOutput) * PhaseUtils.extraCreditScore(gradingContext.phase()));
    }

    @Override
    protected Rubric.RubricType rubricType() {
        return Rubric.RubricType.EXTRA_CREDIT;
    }

    @Override
    protected Set<String> modulesToCheckCoverage() {
        return Set.of();
    }
}
