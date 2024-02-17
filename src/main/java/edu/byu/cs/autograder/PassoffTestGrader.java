package edu.byu.cs.autograder;

import edu.byu.cs.canvas.Rubric;
import edu.byu.cs.model.Phase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;

public abstract class PassoffTestGrader extends Grader {

    private static final Logger LOGGER = LoggerFactory.getLogger(PassoffTestGrader.class);

    /**
     * The path where the official tests are stored
     */
    protected final File phaseTests;

    /**
     * The path where the compiled tests are stored (and ran)
     */
    private final File stageTestsPath;

    /**
     * The module to compile during this test
     */
    private final String module;

    /**
     * The names of the test files with extra credit tests (excluding .java)
     */
    protected Set<String> extraCreditTests = new HashSet<>();

    /**
     * The value (in percentage) of each extra credit category
     */
    protected float extraCreditValue = 0;

    /**
     * Creates a new grader for phase X
     *
     * @param phaseResources the path to the phase resources
     * @param netId          the netId of the student
     * @param repoUrl        the url of the student repo
     * @param observer       the observer to notify of updates
     * @param phase          the phase to grade
     * @throws IOException if an IO error occurs
     */
    public PassoffTestGrader(String phaseResources, String netId, String repoUrl, Observer observer, Phase phase) throws IOException {
        super(repoUrl, netId, observer, phase);
        this.stageTestsPath = new File(stagePath + "/tests");
        this.phaseTests = new File(phaseResources);
        // FIXME
        this.module = switch (phase) {
            case Phase0, Phase1 -> "shared";
            case Phase3, Phase4 -> "server";
            case Phase6 -> "client";
        };
    }

    @Override
    protected TestAnalyzer.TestNode runCustomTests() {
        // no unit tests for this phase
        return null;
    }

    @Override
    protected void compileTests() {
        observer.update("Compiling tests...");
        new TestHelper().compileTests(stageRepo, module, phaseTests, stagePath, new HashSet<>());
        observer.update("Finished compiling tests.");
    }

    @Override
    protected Rubric.Results runTests() {
        observer.update("Running tests...");

        TestAnalyzer.TestNode results = new TestHelper().runJUnitTests(
                new File(stageRepo, "/" + module + "/target/" + module + "-jar-with-dependencies.jar"),
                stageTestsPath,
                extraCreditTests
        );

        results.testName = PASSOFF_TESTS_NAME;

        float score = getScore(results);

        return new Rubric.Results("", score, results, null);
    }

    @Override
    protected float getScore(TestAnalyzer.TestNode results) {
        if (results == null)
            return 0;

        float totalStandardTests = results.numTestsFailed + results.numTestsPassed;
        float totalECTests = results.numExtraCreditPassed + results.numExtraCreditFailed;

        if (totalStandardTests == 0)
            return 0;

        float score = results.numTestsPassed / totalStandardTests;
        if (totalECTests == 0) return score;

        // extra credit calculation
        if (score < 1f) return score;
        Map<String, Float> ecScores = getECScores(results);
        for (String category : extraCreditTests) {
            if (ecScores.get(category) == 1f) {
                score += extraCreditValue;
            }
        }

        return score;
    }

    private Map<String, Float> getECScores(TestAnalyzer.TestNode results) {
        Map<String, Float> scores = new HashMap<>();

        Queue<TestAnalyzer.TestNode> unchecked = new PriorityQueue<>();
        unchecked.add(results);

        while (!unchecked.isEmpty()) {
            TestAnalyzer.TestNode node = unchecked.remove();
            for (TestAnalyzer.TestNode child : node.children.values()) {
                if (child.ecCategory != null) {
                    scores.put(child.ecCategory, (float) child.numExtraCreditPassed /
                            (child.numExtraCreditPassed + child.numExtraCreditFailed));
                    unchecked.remove(child);
                } else unchecked.add(child);
            }
        }

        return scores;
    }

    @Override
    protected String getNotes(TestAnalyzer.TestNode results, int numDaysLate) {

        TestAnalyzer.TestNode passoffTests = results.children.get(PASSOFF_TESTS_NAME);
        TestAnalyzer.TestNode customTests = results.children.get(CUSTOM_TESTS_NAME);

        if (passoffTests == null)
            return "No tests were run";

        boolean passed = passoffTests.numTestsFailed == 0;
        if (customTests != null)
            passed = passed && customTests.numTestsFailed == 0;

        if (passed && numDaysLate == 0)
            return "All tests passed";

        if (passed & numDaysLate > 0)
            return "All tests passed, but " + numDaysLate + " day"+ (numDaysLate > 1 ? "s" : "") +" late. (-" + Math.min(50, numDaysLate * 10) + "%)";

        return "Some tests failed. You must pass all tests to pass off this phase";
    }

    @Override
    protected Rubric.Results runQualityChecks() {
        return null;
    }
}
