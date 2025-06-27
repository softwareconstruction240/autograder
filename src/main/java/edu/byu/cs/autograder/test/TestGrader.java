package edu.byu.cs.autograder.test;

import edu.byu.cs.autograder.GradingContext;
import edu.byu.cs.autograder.GradingException;
import edu.byu.cs.dataAccess.DaoService;
import edu.byu.cs.dataAccess.DataAccessException;
import edu.byu.cs.model.*;
import edu.byu.cs.util.PhaseUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

/**
 * This abstract base class is responsible for running the tests and giving a score
 * based on the output of the tests. In addition to the normal tests, the
 * {@code TestGrader} will also run any extra credit tests and will also check a specified
 * set of tests for code coverage. The {@code TestGrader} will only run tests for one module,
 * which is defined at instantiation.
 * <br>
 * What tests run, what score to apply, and what set of tests code coverage is tested for
 * is determined and must be defined by the subclass extending the {@code TestGrader}.
 */
public abstract class TestGrader {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestGrader.class);

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
    protected final String module;

    protected final GradingContext gradingContext;

    protected final TestHelper testHelper = new TestHelper();

    public TestGrader(GradingContext gradingContext) {
        this.gradingContext = gradingContext;
        this.stageTestsPath = new File(gradingContext.stagePath() + "/tests");
        this.phaseTests = new File("./phases/phase" + PhaseUtils.getPhaseAsString(gradingContext.phase()));
        this.module = PhaseUtils.getModuleUnderTest(gradingContext.phase());
    }


    /**
     * Compiles, runs, and scores the tests provided to the {@code TestGrader}
     *
     * @return the results as a {@link Rubric.Results}
     * @throws GradingException if there was an issue compiling, running, or scoring the tests
     * @throws DataAccessException if there was an issue getting the rubric config for the phase
     */
    public Rubric.Results runTests() throws GradingException, DataAccessException {
        compileTests();
        gradingContext.observer().update("Running " + name() + " tests...");

        TestOutput results;
        if (!new File(gradingContext.stagePath(), "tests").exists()) {
            results = new TestOutput(new TestNode(), new CoverageAnalysis(new HashSet<>()), null);
            results.root().countTests();
        } else {
            results = new TestHelper().runJUnitTests(new File(gradingContext.stageRepo(),
                            "/" + module + "/target/" + module + "-test-dependencies.jar"), stageTestsPath,
                    packagesToTest(), modulesToCheckCoverage());
        }

        if (results.root() == null) {
            results = new TestOutput(new TestNode(), new CoverageAnalysis(new HashSet<>()), results.error());
            results.root().countTests();
            LOGGER.error("{} tests failed to run for {} in phase {}", name(), gradingContext.netId(),
                    PhaseUtils.getPhaseAsString(gradingContext.phase()));
        }

        results.root().setTestName(testName());

        String notes = getNotes(results);
        float score = getScore(results);
        RubricConfig rubricConfig = DaoService.getRubricConfigDao().getRubricConfig(gradingContext.phase());
        RubricConfig.RubricConfigItem configItem = rubricConfig.items().get(rubricType());
        int possiblePoints = configItem != null ? configItem.points() : 0;

        return new Rubric.Results(notes, score, possiblePoints, results, null);
    }

    private void compileTests() throws GradingException {
        gradingContext.observer().update("Compiling " + name() + " tests...");
        testHelper.compileTests(gradingContext.stageRepo(), module, testsToCompile(), gradingContext.stagePath());
    }

    protected abstract String name();

    protected abstract Set<File> testsToCompile() throws GradingException;

    protected abstract Set<String> packagesToTest() throws GradingException;

    protected abstract String testName();

    protected abstract float getScore(TestOutput testResults) throws GradingException;

    protected abstract String getNotes(TestOutput testResults) throws GradingException;

    protected abstract Rubric.RubricType rubricType();

    protected abstract Set<String> modulesToCheckCoverage() throws GradingException;

}
