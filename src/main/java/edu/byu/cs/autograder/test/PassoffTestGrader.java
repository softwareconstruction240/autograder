package edu.byu.cs.autograder.test;

import edu.byu.cs.autograder.GradingContext;
import edu.byu.cs.autograder.GradingException;
import edu.byu.cs.model.Phase;
import edu.byu.cs.model.RubricConfig;
import edu.byu.cs.util.PhaseUtils;

import java.io.File;
import java.util.*;

public class PassoffTestGrader extends TestGrader {

    /**
     * The names of the test files with extra credit tests (excluding .java)
     */
    protected final Set<String> extraCreditTests = new HashSet<>();

    /**
     * The value (in percentage) of each extra credit category
     */
    protected float extraCreditValue = 0;


    public PassoffTestGrader(GradingContext gradingContext) {
        super(gradingContext);
        if (gradingContext.phase() == Phase.Phase1) {
            extraCreditTests.add("CastlingTests");
            extraCreditTests.add("EnPassantTests");
            extraCreditValue = .04f;
        }
    }

    @Override
    protected String name() {
        return "passoff";
    }

    @Override
    protected Set<String> excludedTests() {
        return new HashSet<>();
    }

    @Override
    protected File testsToCompile() {
        return phaseTests;
    }

    @Override
    protected Set<String> packagesToTest() throws GradingException {
        return PhaseUtils.passoffPackagesToTest(gradingContext.phase());
    }

    @Override
    protected Set<String> extraCreditTests() {
        return extraCreditTests;
    }

    @Override
    protected String testName() {
        return "Passoff Tests";
    }

    @Override
    protected float getScore(TestAnalyzer.TestNode testResults) {
        float totalStandardTests = testResults.getNumTestsFailed() + testResults.getNumTestsPassed();
        float totalECTests = testResults.getNumExtraCreditPassed() + testResults.getNumExtraCreditFailed();

        if (totalStandardTests == 0) return 0;

        float score = testResults.getNumTestsPassed() / totalStandardTests;
        if (totalECTests == 0) return score;

        // extra credit calculation
        if (score < 1f) return score;
        Map<String, Float> ecScores = getECScores(testResults);
        for (String category : extraCreditTests()) {
            if (ecScores.get(category) == 1f) {
                score += extraCreditValue;
            }
        }

        return score;
    }

    @Override
    protected String getNotes(TestAnalyzer.TestNode testResults) {
        StringBuilder notes = new StringBuilder();

        if (testResults == null) return "No tests were run";

        if (testResults.getNumTestsFailed() == 0) notes.append("All required tests passed");
        else notes.append("Some required tests failed");

        Map<String, Float> ecScores = getECScores(testResults);
        float totalECPoints = ecScores.values().stream().reduce(0f, Float::sum) * extraCreditValue;

        if (totalECPoints > 0f) notes.append("\nExtra credit tests: +").append(totalECPoints * 100).append("%");

        return notes.toString();
    }

    @Override
    protected RubricConfig.RubricConfigItem rubricConfigItem(RubricConfig config) {
        return config.passoffTests();
    }


    private Map<String, Float> getECScores(TestAnalyzer.TestNode results) {
        Map<String, Float> scores = new HashMap<>();

        Queue<TestAnalyzer.TestNode> unchecked = new PriorityQueue<>();
        unchecked.add(results);

        while (!unchecked.isEmpty()) {
            TestAnalyzer.TestNode node = unchecked.remove();
            for (TestAnalyzer.TestNode child : node.getChildren().values()) {
                if (child.getEcCategory() != null) {
                    scores.put(child.getEcCategory(), (float) child.getNumExtraCreditPassed() /
                            (child.getNumExtraCreditPassed() + child.getNumExtraCreditFailed()));
                    unchecked.remove(child);
                } else unchecked.add(child);
            }
        }

        return scores;
    }
}
