package edu.byu.cs.autograder;

import edu.byu.cs.autograder.compile.CompileHelper;
import edu.byu.cs.autograder.database.DatabaseHelper;
import edu.byu.cs.autograder.git.GitHelper;
import edu.byu.cs.autograder.score.Scorer;
import edu.byu.cs.model.*;
import edu.byu.cs.dataAccess.DaoService;
import edu.byu.cs.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.time.Instant;
import java.util.*;

/**
 * A template for fetching, compiling, and running student code
 */
public abstract class Grader implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(Grader.class);

    protected static final String PASSOFF_TESTS_NAME = "Passoff Tests";
    protected static final String CUSTOM_TESTS_NAME = "Custom Tests";

    private final DatabaseHelper dbHelper;

    private final GitHelper gitHelper;

    private final CompileHelper compileHelper;

    protected final GradingContext gradingContext;

    protected Observer observer;

    /**
     * Creates a new grader
     *
     * @param repoUrl  the url of the student repo
     * @param netId    the netId of the student
     * @param observer the observer to notify of updates
     * @param phase    the phase to grade
     */
    public Grader(String repoUrl, String netId, Observer observer, Phase phase) throws IOException {
        String phasesPath = new File("./phases").getCanonicalPath();
        long salt = Instant.now().getEpochSecond();
        String stagePath = new File("./tmp-" + repoUrl.hashCode() + "-" + salt).getCanonicalPath();
        File stageRepo = new File(stagePath, "repo");
        int requiredCommits = 10;
        this.observer = observer;
        this.gradingContext =
                new GradingContext(netId, phase, phasesPath, stagePath, repoUrl, stageRepo, requiredCommits, observer);
        this.dbHelper = new DatabaseHelper(salt, gradingContext);
        this.gitHelper = new GitHelper(gradingContext);
        this.compileHelper = new CompileHelper(gradingContext);
    }

    public void run() {
        observer.notifyStarted();
        try {
            // FIXME: remove this sleep. currently the grader is too quick for the client to keep up
            Thread.sleep(1000);
            int numCommits = gitHelper.setUp();
            dbHelper.setUp();
            compileHelper.compile();

            RubricConfig rubricConfig = DaoService.getRubricConfigDao().getRubricConfig(gradingContext.phase());
            Rubric.Results qualityResults = null;
            if(rubricConfig.quality() != null) {
                qualityResults = runQualityChecks();
            }


            Rubric.Results passoffResults = null;
            if(rubricConfig.passoffTests() != null) {
                compileTests();
                passoffResults = runTests(getPackagesToTest());
            }
            Rubric.Results customTestsResults = null;
            if(rubricConfig.unitTests() != null) {
                customTestsResults = runCustomTests();
            }

            dbHelper.finish();

            Rubric.RubricItem qualityItem = null;
            Rubric.RubricItem passoffItem = null;
            Rubric.RubricItem customTestsItem = null;

            if (qualityResults != null)
                qualityItem = new Rubric.RubricItem(rubricConfig.quality().category(), qualityResults, rubricConfig.quality().criteria());
            if (passoffResults != null)
                passoffItem = new Rubric.RubricItem(rubricConfig.passoffTests().category(), passoffResults, rubricConfig.passoffTests().criteria());
            if (customTestsResults != null)
                customTestsItem = new Rubric.RubricItem(rubricConfig.unitTests().category(), customTestsResults, rubricConfig.unitTests().criteria());

            Rubric rubric = new Rubric(passoffItem, customTestsItem, qualityItem, false, "");

            Submission submission = new Scorer(gradingContext).score(rubric, numCommits);

            observer.notifyDone(submission);

        } catch (GradingException ge) {
            observer.notifyError(ge.getMessage(), ge.getDetails());
            String notification =
                    "Error running grader for user " + gradingContext.netId() + " and repository " + gradingContext.repoUrl();
            if(ge.getDetails() != null) notification += ". Details:\n" + ge.getDetails();
            LOGGER.error(notification, ge);
        }
        catch (Exception e) {
            observer.notifyError(e.getMessage());
            LOGGER.error("Error running grader for user " + gradingContext.netId() + " and repository " + gradingContext.repoUrl(), e);
        } finally {
            dbHelper.cleanUp();
            FileUtils.removeDirectory(new File(gradingContext.stagePath()));
        }
    }

    private Set<String> getPackagesToTest() throws GradingException {
        return switch (gradingContext.phase()) {
            case Phase0 -> Set.of("passoffTests.chessTests", "passoffTests.chessTests.chessPieceTests");
            case Phase1 -> Set.of("passoffTests.chessTests", "passoffTests.chessTests.chessExtraCredit");
            case Phase3, Phase4 -> Set.of("passoffTests.serverTests");
            case Phase5 -> throw new GradingException("No passoff tests for this phase");
            case Phase6 -> throw new GradingException("Not implemented");
        };
    }


    /**
     * Runs quality checks on the student's code
     *
     * @return the results of the quality checks as a CanvasIntegration.RubricItem
     */
    protected Rubric.Results runQualityChecks() throws GradingException {
        RubricConfig rubricConfig = DaoService.getRubricConfigDao().getRubricConfig(gradingContext.phase());
        if(rubricConfig.quality() == null) return null;
        observer.update("Running code quality...");

        QualityAnalyzer analyzer = new QualityAnalyzer();

        QualityAnalyzer.QualityOutput quality = analyzer.runQualityChecks(gradingContext.stageRepo());

        return new Rubric.Results(quality.notes(), quality.score(),
                rubricConfig.quality().points(), null, quality.results());
    }


    /**
     * Run the unit tests written by the student. This approach is destructive as it will delete non-unit tests
     *
     * @return the results of the tests
     */
    protected abstract Rubric.Results runCustomTests() throws GradingException;

    /**
     * Compiles the test files with the student code
     */
    protected abstract void compileTests() throws GradingException;

    /**
     * Runs the tests on the student code
     */
    protected abstract Rubric.Results runTests(Set<String> packagesToTest) throws GradingException;

    public interface Observer {
        void notifyStarted();

        void update(String message);

        void notifyError(String message);
        void notifyError(String message, String details);

        void notifyDone(Submission submission);
    }

}
