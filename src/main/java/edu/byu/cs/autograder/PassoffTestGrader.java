package edu.byu.cs.autograder;

import edu.byu.cs.dataAccess.DaoService;
import edu.byu.cs.model.Phase;
import edu.byu.cs.model.Rubric;
import edu.byu.cs.model.RubricConfig;
import edu.byu.cs.util.PhaseUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class PassoffTestGrader extends Grader {
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
     * @param netId          the netId of the student
     * @param repoUrl        the url of the student repo
     * @param observer       the observer to notify of updates
     * @param phase          the phase to grade
     * @throws IOException if an IO error occurs
     */
    public PassoffTestGrader(String netId, String repoUrl, Observer observer, Phase phase)
            throws IOException {
        super(repoUrl, netId, observer, phase);
        this.stageTestsPath = new File(gradingContext.stagePath() + "/tests");
        this.phaseTests = new File("./phases/phase" + PhaseUtils.getPhaseAsString(phase));
        // FIXME
        this.module = switch (phase) {
            case Phase0, Phase1 -> "shared";
            case Phase3, Phase4 -> "server";
            case Phase5, Phase6 -> "client";
        };

        initializeExtraCredit();
    }

    private void initializeExtraCredit() {
        if(gradingContext.phase() == Phase.Phase1) {
            extraCreditTests.add("CastlingTests");
            extraCreditTests.add("EnPassantTests");
            extraCreditValue = .04f;
        }
    }

    @Override
    protected Rubric.Results runCustomTests() throws GradingException {
        Set<String> excludedTests = new TestHelper().getTestFileNames(phaseTests);
        new TestHelper().compileTests(gradingContext.stageRepo(), module,
                new File(gradingContext.stageRepo(), module + "/src/test/java/"), gradingContext.stagePath(),
                excludedTests);

        TestAnalyzer.TestAnalysis results;
        if (!new File(gradingContext.stagePath(), "tests").exists()) {
            results = new TestAnalyzer.TestAnalysis(new TestAnalyzer.TestNode(), null);
            TestAnalyzer.TestNode.countTests(results.root());
        } else results = new TestHelper().runJUnitTests(
                new File(gradingContext.stageRepo(), "/" + module + "/target/server-jar-with-dependencies.jar"),
                new File(gradingContext.stagePath(), "tests"),
                unitTestPackages(gradingContext.phase()),
                new HashSet<>());

        if (results.root() == null) {
            results = new TestAnalyzer.TestAnalysis(new TestAnalyzer.TestNode(), results.error());
            TestAnalyzer.TestNode.countTests(results.root());
            LOGGER.error("Tests failed to run for " + gradingContext.netId() + " in phase " +
                    PhaseUtils.getPhaseAsString(gradingContext.phase()));
        }

        results.root().testName = CUSTOM_TESTS_NAME;

        RubricConfig rubricConfig = DaoService.getRubricConfigDao().getRubricConfig(gradingContext.phase());

        return new Rubric.Results(getUnitTestNotes(results.root()), getUnitTestScore(results.root()),
                rubricConfig.unitTests().points(), results, null);
    }

    protected float getUnitTestScore(TestAnalyzer.TestNode testResults) throws GradingException {
        float totalTests = testResults.numTestsFailed + testResults.numTestsPassed;

        if (totalTests == 0) return 0;

        int minTests = minUnitTests(gradingContext.phase());

        if (totalTests < minTests) return (float) testResults.numTestsPassed / minTests;

        return testResults.numTestsPassed / totalTests;
    }

    private int minUnitTests(Phase phase) throws GradingException {
        return switch (phase) {
            case Phase0, Phase1, Phase6 -> throw new GradingException("No unit tests for this phase");
            case Phase3 -> 13;
            case Phase4 -> 18;
            case Phase5 -> 12;
        };
    }

    private Set<String> unitTestPackages(Phase phase) throws GradingException {
        return switch (phase) {
            case Phase0, Phase1, Phase6 -> throw new GradingException("No unit tests for this phase");
            case Phase3 -> Set.of("serviceTests");
            case Phase4 -> Set.of("dataAccessTests");
            case Phase5 -> Set.of("clientTests");
        };
    }

    @Override
    protected void compileTests() throws GradingException {
        observer.update("Compiling tests...");
        new TestHelper().compileTests(gradingContext.stageRepo(), module, phaseTests, gradingContext.stagePath(),
                new HashSet<>());
        observer.update("Finished compiling tests.");
    }

    @Override
    protected Rubric.Results runTests(Set<String> packagesToTest) throws GradingException {
        observer.update("Running tests...");

        TestAnalyzer.TestAnalysis results = new TestHelper().runJUnitTests(
                new File(gradingContext.stageRepo(), "/" + module + "/target/" + module + "-jar-with-dependencies.jar"),
                stageTestsPath, packagesToTest, extraCreditTests);

        if (results.root() == null) {
            results = new TestAnalyzer.TestAnalysis(new TestAnalyzer.TestNode(), results.error());
            TestAnalyzer.TestNode.countTests(results.root());
            LOGGER.error("Passoff tests failed to run for " + gradingContext.netId() + " in phase " +
                    gradingContext.phase());
        }

        results.root().testName = PASSOFF_TESTS_NAME;

        float score = getPassoffScore(results.root());
        RubricConfig rubricConfig = DaoService.getRubricConfigDao().getRubricConfig(gradingContext.phase());

        return new Rubric.Results(getPassoffTestNotes(results.root()), score, rubricConfig.passoffTests().points(),
                results, null);
    }

    protected float getPassoffScore(TestAnalyzer.TestNode testResults) {
        float totalStandardTests = testResults.numTestsFailed + testResults.numTestsPassed;
        float totalECTests = testResults.numExtraCreditPassed + testResults.numExtraCreditFailed;

        if (totalStandardTests == 0) return 0;

        float score = testResults.numTestsPassed / totalStandardTests;
        if (totalECTests == 0) return score;

        // extra credit calculation
        if (score < 1f) return score;
        Map<String, Float> ecScores = getECScores(testResults);
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

    protected String getPassoffTestNotes(TestAnalyzer.TestNode results) {

        StringBuilder notes = new StringBuilder();

        if (results == null) return "No tests were run";

        if (results.numTestsFailed == 0) notes.append("All required tests passed");
        else notes.append("Some required tests failed");

        Map<String, Float> ecScores = getECScores(results);
        float totalECPoints = ecScores.values().stream().reduce(0f, Float::sum) * extraCreditValue;

        if (totalECPoints > 0f) notes.append("\nExtra credit tests: +").append(totalECPoints * 100).append("%");

        return notes.toString();
    }

    protected String getUnitTestNotes(TestAnalyzer.TestNode testResults) throws GradingException {
        if (testResults.numTestsPassed + testResults.numTestsFailed < minUnitTests(gradingContext.phase()))
            return "Not enough tests: each " + codeUnderTest(gradingContext.phase()) +
                    " method should have a positive and negative test";

        return switch (testResults.numTestsFailed) {
            case 0 -> "All tests passed";
            case 1 -> "1 test failed";
            default -> testResults.numTestsFailed + " tests failed";
        };
    }

    private String codeUnderTest(Phase phase) throws GradingException {
        return switch (phase) {
            case Phase0, Phase1, Phase6 -> throw new GradingException("No unit tests for this phase");
            case Phase3 -> "service";
            case Phase4 -> "dao";
            case Phase5 -> "server facade";
        };
    }
}
