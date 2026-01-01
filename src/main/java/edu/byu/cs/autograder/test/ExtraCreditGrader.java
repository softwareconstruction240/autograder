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
        float score = 0;

        Map<String, Float> ecScores = getECScores(testOutput.root());
        float extraCreditValue = PhaseUtils.extraCreditValue(gradingContext.phase());
        for (Float ecScore : ecScores.values()) {
            if (ecScore == 1f) {
                score += extraCreditValue;
            }
        }

        return score;
    }

    @Override
    protected String getNotes(TestOutput testOutput) {
        TestNode testResults = testOutput.root();
        StringBuilder notes = new StringBuilder();

        if (testResults == null) return "No tests were run";

        Map<String, Float> ecScores = getECScores(testOutput.root());
        float extraCreditValue = PhaseUtils.extraCreditValue(gradingContext.phase());
        float totalECPoints = ecScores.values().stream().reduce(0f, (f1, f2) -> (float) (f1 + Math.floor(f2))) * extraCreditValue;

        if (totalECPoints > 0f) notes.append("Extra credit tests: +").append(totalECPoints * 100).append("%");

        return notes.toString();
    }

    private Map<String, Float> getECScores(TestNode results) {
        Map<String, Float> scores = new HashMap<>();

        for (TestNode child : results.getChildren().values()) {
            scores.put(child.getTestName(), (float) child.getNumTestsPassed() / child.getNumTestsTotal());
        }

        return scores;
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
