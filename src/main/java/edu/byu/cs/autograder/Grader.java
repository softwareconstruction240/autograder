package edu.byu.cs.autograder;

import edu.byu.cs.analytics.CommitAnalytics;
import edu.byu.cs.canvas.CanvasException;
import edu.byu.cs.canvas.CanvasIntegration;
import edu.byu.cs.canvas.CanvasUtils;
import edu.byu.cs.model.*;
import edu.byu.cs.dataAccess.DaoService;
import edu.byu.cs.dataAccess.SubmissionDao;
import edu.byu.cs.dataAccess.UserDao;
import edu.byu.cs.util.DateTimeUtils;
import edu.byu.cs.util.FileUtils;
import edu.byu.cs.util.PhaseUtils;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;

/**
 * A template for fetching, compiling, and running student code
 */
public abstract class Grader implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(Grader.class);

    /**
     * The netId of the student
     */
    protected final String netId;

    /**
     * The phase to grade
     */
    protected final Phase phase;

    /**
     * The path where the official tests are stored
     */
    protected final String phasesPath;

    /**
     * The path where JUnit jars are stored
     */
    protected final String libsDir;

    /**
     * The path to the standalone JUnit jar
     */
    protected final String standaloneJunitJarPath;

    /**
     * The path to the JUnit Jupiter API jar
     */
    protected final String junitJupiterApiJarPath;

    /**
     * The path to the passoff dependencies jar
     */
    protected final String passoffDependenciesPath;

    /**
     * The path for the student repo to be put in and tested
     */
    protected final String stagePath;

    /**
     * The url of the student repo
     */
    private final String repoUrl;


    /**
     * The path for the student repo (child of stagePath)
     */

    protected final File stageRepo;

    /**
     * The required number of commits (since the last phase) to be able to pass off
     */
    private final int requiredCommits;

    protected final String PASSOFF_TESTS_NAME = "Passoff Tests";
    protected final String CUSTOM_TESTS_NAME = "Custom Tests";

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
        this.netId = netId;
        this.phase = phase;
        this.phasesPath = new File("./phases").getCanonicalPath();
        this.libsDir = new File(phasesPath, "libs").getCanonicalPath();
        this.standaloneJunitJarPath = new File(libsDir, "junit-platform-console-standalone-1.10.1.jar").getCanonicalPath();
        this.junitJupiterApiJarPath = new File(libsDir, "junit-jupiter-api-5.10.1.jar").getCanonicalPath();
        this.passoffDependenciesPath = new File(libsDir, "passoff-dependencies.jar").getCanonicalPath();

        this.stagePath = new File("./tmp-" + repoUrl.hashCode() + "-" + Instant.now().getEpochSecond()).getCanonicalPath();

        this.repoUrl = repoUrl;
        this.stageRepo = new File(stagePath, "repo");

        this.requiredCommits = 10;

        this.observer = observer;
    }

    public void run() {
        observer.notifyStarted();

        try {
            // FIXME: remove this sleep. currently the grader is too quick for the client to keep up
            Thread.sleep(1000);
            fetchRepo();
            int numCommits = verifyRegularCommits();
            verifyProjectStructure();
            packageRepo();

            Rubric.Results qualityResults = runQualityChecks();
            compileTests();
            Rubric.Results passoffResults = runTests();
            Rubric.Results customTestsResults = runCustomTests();

            Rubric.RubricItem qualityItem = null;
            Rubric.RubricItem passoffItem = null;
            Rubric.RubricItem customTestsItem = null;

            RubricConfig rubricConfig = DaoService.getRubricConfigDao().getRubricConfig(phase);
            if (qualityResults != null)
                qualityItem = new Rubric.RubricItem(rubricConfig.quality().category(), qualityResults, rubricConfig.quality().criteria());
            if (passoffResults != null)
                passoffItem = new Rubric.RubricItem(rubricConfig.passoffTests().category(), passoffResults, rubricConfig.passoffTests().criteria());
            if (customTestsResults != null)
                customTestsItem = new Rubric.RubricItem(rubricConfig.unitTests().category(), customTestsResults, rubricConfig.unitTests().criteria());

            Rubric rubric = new Rubric(passoffItem, customTestsItem, qualityItem);
            rubric = CanvasUtils.decimalScoreToPoints(phase, rubric);

            saveResults(rubric, numCommits);
            observer.notifyDone(rubric);

        } catch (Exception e) {
            observer.notifyError(e.getMessage());

            LOGGER.error("Error running grader for user " + netId + " and repository " + repoUrl, e);
        } finally {
            FileUtils.removeDirectory(new File(stagePath));
        }
    }

    /**
     * Runs quality checks on the student's code
     *
     * @return the results of the quality checks as a CanvasIntegration.RubricItem
     */
    protected abstract Rubric.Results runQualityChecks();

    /**
     * Verifies that the project is structured correctly. The project should be at the top level of the git repository,
     * which is checked by looking for a pom.xml file
     */
    private void verifyProjectStructure() {
        File pomFile = new File(stageRepo, "pom.xml");
        if (!pomFile.exists()) {
            observer.notifyError("Project is not structured correctly. Your project should be at the top level of your git repository.");
            throw new RuntimeException("No pom.xml file found");
        }
    }

    /**
     * Saves the results of the grading to the database and to Canvas if the submission passed
     *
     * @param rubric the rubric for the phase
     */
    private void saveResults(Rubric rubric, int numCommits) {
        String headHash = getHeadHash();

        int assignmentNum = PhaseUtils.getPhaseAssignmentNumber(phase);

        int canvasUserId = DaoService.getUserDao().getUser(netId).canvasUserId();

        ZonedDateTime dueDate;
        try {
            dueDate = CanvasIntegration.getAssignmentDueDateForStudent(canvasUserId, assignmentNum);
        } catch (CanvasException e) {
            throw new RuntimeException("Failed to get due date for assignment " + assignmentNum + " for user " + netId, e);
        }

        // penalize at most 5 days
        ZonedDateTime handInDate = DaoService.getQueueDao().get(netId).timeAdded().atZone(ZoneId.of("America/Denver"));
        int numDaysLate = Math.min(DateTimeUtils.getNumDaysLate(handInDate, dueDate), 5);
        float score = getScore(rubric);
        score -= numDaysLate * 0.1F;
        if (score < 0) score = 0;

        SubmissionDao submissionDao = DaoService.getSubmissionDao();
        Submission submission = new Submission(
                netId,
                repoUrl,
                headHash,
                handInDate.toInstant(),
                phase,
                passed(rubric),
                score,
                numCommits,
                "",
                rubric
        );

        if (submission.passed()) {
            sendToCanvas(submission);
        }

        submissionDao.insertSubmission(submission);
    }

    private void sendToCanvas(Submission submission) {
        UserDao userDao = DaoService.getUserDao();
        User user = userDao.getUser(netId);

        int userId = user.canvasUserId();

        int assignmentNum = PhaseUtils.getPhaseAssignmentNumber(phase);

        float score = submission.score() * PhaseUtils.getTotalPoints(phase);

        try {
            CanvasIntegration.submitGrade(userId, assignmentNum, score, submission.notes());
        } catch (CanvasException e) {
            LOGGER.error("Error submitting score for user " + submission.netId(), e);
        }

    }

    private String getHeadHash() {
        String headHash;
        try (Git git = Git.open(stageRepo)) {
            headHash = git.getRepository().findRef("HEAD").getObjectId().getName();
        } catch (IOException e) {
            throw new RuntimeException("Failed to get head hash: " + e.getMessage());
        }
        return headHash;
    }

    /**
     * Fetches the student repo and puts it in the given local path
     */
    private void fetchRepo() {
        observer.update("Fetching repo...");

        CloneCommand cloneCommand = Git.cloneRepository()
                .setURI(repoUrl)
                .setDirectory(stageRepo);

        try (Git git = cloneCommand.call()) {
            LOGGER.info("Cloned repo to " + git.getRepository().getDirectory());
        } catch (GitAPIException e) {
            observer.notifyError("Failed to clone repo: " + e.getMessage());
            LOGGER.error("Failed to clone repo", e);
            throw new RuntimeException("Failed to clone repo: " + e.getMessage());
        }

        observer.update("Successfully fetched repo");
    }

    /**
     * Counts the commits since the last passoff and halts progress if there are less than the required amount
     *
     * @return the number of commits since the last passoff
     */
    private int verifyRegularCommits() {
        observer.update("Verifying commits...");

        try (Git git = Git.open(stageRepo)) {
            Iterable<RevCommit> commits = git.log().all().call();
            Submission submission = DaoService.getSubmissionDao().getFirstPassingSubmission(netId, phase);
            long timestamp = submission == null ? 0L : submission.timestamp().getEpochSecond();
            Map<String, Integer> commitHistory = CommitAnalytics.handleCommits(commits, timestamp, Instant.now().getEpochSecond());
            int numCommits = CommitAnalytics.getTotalCommits(commitHistory);
//            if (numCommits < requiredCommits) {
//                observer.notifyError("Not enough commits to pass off. (" + numCommits + "/" + requiredCommits + ")");
//                LOGGER.error("Insufficient commits to pass off.");
//                throw new RuntimeException("Not enough commits to pass off");
//            }

            return numCommits;
        } catch (IOException | GitAPIException e) {
            observer.notifyError("Failed to count commits: " + e.getMessage());
            LOGGER.error("Failed to count commits", e);
            throw new RuntimeException("Failed to count commits: " + e.getMessage());
        }
    }

    /**
     * Packages the student repo into a jar
     */
    protected void packageRepo() {
        observer.update("Packaging repo...");

        String[] commands = new String[]{"package"};

        for (String command : commands) {
            observer.update("  Running maven " + command + " command...");
            ProcessBuilder processBuilder = new ProcessBuilder();
            processBuilder.directory(stageRepo);
            processBuilder.command("mvn", command, "-Dmaven.test.skip");
            try {
                processBuilder.inheritIO();
                Process process = processBuilder.start();
                if (process.waitFor() != 0) {
                    observer.notifyError("Failed to " + command + " repo");
                    LOGGER.error("Failed to " + command + " repo");
                    throw new RuntimeException("Failed to " + command + " repo");
                }
            } catch (IOException | InterruptedException ex) {
                observer.notifyError("Failed to " + command + " repo: " + ex.getMessage());
                LOGGER.error("Failed to " + command + " repo", ex);
                throw new RuntimeException("Failed to " + command + " repo", ex);
            }

            observer.update("  Successfully ran maven " + command + " command");
        }

        observer.update("Successfully packaged repo");
    }

    /**
     * Run the unit tests written by the student. This approach is destructive as it will delete non-unit tests
     *
     * @return the results of the tests
     */
    protected abstract Rubric.Results runCustomTests();

    /**
     * Compiles the test files with the student code
     */
    protected abstract void compileTests();

    /**
     * Runs the tests on the student code
     */
    protected abstract Rubric.Results runTests();

    /**
     * Gets the score for the phase
     *
     * @return the score
     */
    protected float getScore(Rubric rubric) {
        int totalPossiblePoints = DaoService.getRubricConfigDao().getPhaseTotalPossiblePoints(phase);

        if (totalPossiblePoints == 0)
            throw new RuntimeException("Total possible points for phase " + phase + " is 0");

        float score = 0;
        if (rubric.passoffTests() != null)
            score += rubric.passoffTests().results().score();

        if (rubric.unitTests() != null)
            score += rubric.unitTests().results().score();

        if (rubric.quality() != null)
            score += rubric.quality().results().score();

        return score / totalPossiblePoints;
    }

    protected abstract boolean passed(Rubric rubric);

    public interface Observer {
        void notifyStarted();

        void update(String message);

        void notifyError(String message);

        void notifyDone(Rubric rubric);
    }

}
