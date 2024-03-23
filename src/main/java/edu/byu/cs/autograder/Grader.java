package edu.byu.cs.autograder;

import edu.byu.cs.analytics.CommitAnalytics;
import edu.byu.cs.autograder.git.GitHelper;
import edu.byu.cs.autograder.score.Scorer;
import edu.byu.cs.model.*;
import edu.byu.cs.dataAccess.DaoService;
import edu.byu.cs.util.FileUtils;
import edu.byu.cs.util.ProcessUtils;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;
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
    }

    public void run() {
        observer.notifyStarted();
        try {
            // FIXME: remove this sleep. currently the grader is too quick for the client to keep up
            Thread.sleep(1000);
            int numCommits = gitHelper.setUp();
            verifyProjectStructure();

            dbHelper.setUp();

            modifyPoms();

            packageRepo();

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

    private void modifyPoms() {
        File oldRootPom = new File(gradingContext.stageRepo(), "pom.xml");
        File oldServerPom = new File(gradingContext.stageRepo(), "server/pom.xml");
        File oldClientPom = new File(gradingContext.stageRepo(), "client/pom.xml");
        File oldSharedPom = new File(gradingContext.stageRepo(), "shared/pom.xml");

        File newRootPom = new File(gradingContext.phasesPath(), "pom/pom.xml");
        File newServerPom = new File(gradingContext.stageRepo(), "pom/server/pom.xml");
        File newClientPom = new File(gradingContext.stageRepo(), "pom/client/pom.xml");
        File newSharedPom = new File(gradingContext.stageRepo(), "pom/shared/pom.xml");

        FileUtils.copyFile(oldRootPom, newRootPom);
        FileUtils.copyFile(oldServerPom, newServerPom);
        FileUtils.copyFile(oldClientPom, newClientPom);
        FileUtils.copyFile(oldSharedPom, newSharedPom);
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
     * Verifies that the project is structured correctly. The project should be at the top level of the git repository,
     * which is checked by looking for a pom.xml file
     */
    private void verifyProjectStructure() throws GradingException {
        File pomFile = new File(gradingContext.stageRepo(), "pom.xml");
        if (!pomFile.exists()) {
            observer.notifyError("Project is not structured correctly. Your project should be at the top level of your git repository.");
            throw new GradingException("No pom.xml file found");
        }
    }


    /**
     * Packages the student repo into a jar
     */
    protected void packageRepo() throws GradingException {
        observer.update("Packaging repo...");

        observer.update("  Running maven package command...");
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.directory(gradingContext.stageRepo());
        processBuilder.command("mvn", "package", "-DskipTests");
        try {
            ProcessUtils.ProcessOutput output = ProcessUtils.runProcess(processBuilder, 90000); //90 seconds
            if (output.statusCode() != 0) {
                throw new GradingException("Failed to package repo: ", getMavenError(output.stdOut()));
            }
        } catch (ProcessUtils.ProcessException ex) {
            throw new GradingException("Failed to package repo", ex);
        }

        observer.update("Successfully packaged repo");
    }

    /**
     * Retrieves maven error output from maven package stdout
     *
     * @param output A string containing maven standard output
     * @return A string containing maven package error lines
     */
    private String getMavenError(String output) {
        StringBuilder builder = new StringBuilder();
        for (String line : output.split("\n")) {
            if (line.contains("[ERROR] -> [Help 1]")) {
                break;
            }

            if(line.contains("[ERROR]")) {
                String trimLine = line.replace(gradingContext.stageRepo().getAbsolutePath(), "");
                builder.append(trimLine).append("\n");
            }
        }
        return builder.toString();
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
