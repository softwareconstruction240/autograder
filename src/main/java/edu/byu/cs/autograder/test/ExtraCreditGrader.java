package edu.byu.cs.autograder.test;

import edu.byu.cs.autograder.GradingContext;
import edu.byu.cs.autograder.GradingException;
import edu.byu.cs.model.Rubric;
import edu.byu.cs.model.TestOutput;
import edu.byu.cs.util.PhaseUtils;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

/**
 * Runs and scores the extra credit tests for the phase a submission is graded for
 */
public class ExtraCreditGrader extends TestGrader {
    public ExtraCreditGrader(GradingContext gradingContext) {
        super(gradingContext);
    }

    @Override
    protected String name() {
        return "extra-credit";
    }

    @Override
    protected Set<File> testsToCompile() throws GradingException {
        return Set.of(phaseTests);
    }

    @Override
    protected Set<String> packagesToTest() throws GradingException {
        return PhaseUtils.extraCreditTests(gradingContext.phase());
    }

    @Override
    protected String testName() {
        return "Extra Credit Tests";
    }

    @Override
    protected float getScore(TestOutput testResults) throws GradingException {
        return 0;
    }

    @Override
    protected String getNotes(TestOutput testResults) throws GradingException {
        return "";
    }

    @Override
    protected Rubric.RubricType rubricType() {
        return null;
    }

    @Override
    protected Set<String> modulesToCheckCoverage() throws GradingException {
        return Set.of();
    }
}
