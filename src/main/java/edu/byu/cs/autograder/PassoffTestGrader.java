package edu.byu.cs.autograder;

import edu.byu.cs.dataAccess.DaoService;
import edu.byu.cs.model.Phase;
import edu.byu.cs.model.Rubric;
import edu.byu.cs.model.RubricConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
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
    protected Rubric.Results runCustomTests() {
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

        float score = getPassoffScore(results);
        RubricConfig rubricConfig = DaoService.getRubricConfigDao().getRubricConfig(phase);

        return new Rubric.Results(getNotes(results), score, rubricConfig.passoffTests().points(), results, null);
    }

    protected float getPassoffScore(TestAnalyzer.TestNode testResults) {
        float totalStandardTests = testResults.numTestsFailed + testResults.numTestsPassed;
        float totalECTests = testResults.numExtraCreditPassed + testResults.numExtraCreditFailed;

        if (totalStandardTests == 0)
            return 0;

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

    protected String getNotes(TestAnalyzer.TestNode results) {

        StringBuilder notes = new StringBuilder();

        if (results == null)
            return "No tests were run";

        if (results.numTestsFailed == 0)
            notes.append("All required tests passed");
        else
            notes.append("Some required tests failed");

        Map<String, Float> ecScores = getECScores(results);
        float totalECPoints = ecScores.values().stream().reduce(0f, Float::sum) * extraCreditValue ;

        if (totalECPoints > 0f)
            notes.append("\nExtra credit tests: +").append(totalECPoints * 100).append("%");

        return notes.toString();
    }

    @Override
    protected Rubric.Results runQualityChecks() {
        RubricConfig rubricConfig = DaoService.getRubricConfigDao().getRubricConfig(phase);
        if(rubricConfig.quality() == null) return null;
        observer.update("Running code quality...");

        QualityAnalyzer analyzer = new QualityAnalyzer();

        QualityAnalyzer.QualityAnalysis analysis = analyzer.runQualityChecks(stageRepo);

        float score = analyzer.getScore(analysis);
        String results = analyzer.getResults(analysis);
        String notes = analyzer.getNotes(analysis);

        return new Rubric.Results(notes, score, rubricConfig.quality().points(), null, results);
    }

    @Override
    protected Rubric annotateRubric(Rubric rubric) {
        return new Rubric(
                rubric.passoffTests(),
                rubric.unitTests(),
                rubric.quality(),
                passed(rubric),
                rubric.notes()
        );
    }

}
