package edu.byu.cs.autograder;

import edu.byu.cs.analytics.CommitAnalytics;
import edu.byu.cs.canvas.CanvasException;
import edu.byu.cs.canvas.CanvasIntegration;
import edu.byu.cs.dataAccess.DaoService;
import edu.byu.cs.dataAccess.SubmissionDao;
import edu.byu.cs.dataAccess.UserDao;
import edu.byu.cs.model.Phase;
import edu.byu.cs.model.Submission;
import edu.byu.cs.model.User;
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
import java.util.*;

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
    protected final String stageRepoPath;

    /**
     * Holds configurable settings related to the grading of assignments.
     */
    private final GradingSettings gradingSettings;

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
        this.stageRepoPath = new File(stagePath, "repo").getCanonicalPath();

        this.gradingSettings = this.getGradingSettings();

        this.observer = observer;
    }
    private GradingSettings getGradingSettings() {
        // FIXME! Import from some dynamic location
        return new GradingSettings(
                5,
                10,
                10
        );
    }

    public void run() {
        observer.notifyStarted();

        try {
            // FIXME: remove this sleep. currently the grader is too quick for the client to keep up
            Thread.sleep(1000);
            fetchRepo();
            int numCommits = verifyRegularCommits();
            verifyProjectStructure();
            runCustomTests();
            packageRepo();
            compileTests();
            TestAnalyzer.TestNode results = runTests();
            saveResults(results, numCommits);
            observer.notifyDone(results);

        } catch (Exception e) {
            observer.notifyError(e.getMessage());

            LOGGER.error("Error running grader for user " + netId + " and repository " + repoUrl, e);
        } finally {
            FileUtils.removeDirectory(new File(stagePath));
        }
    }

    private void verifyProjectStructure() {
        File pomFile = new File(stageRepoPath, "pom.xml");
        if (!pomFile.exists()) {
            observer.notifyError("Project is not structured correctly. Your project should be at the top level of your git repository.");
            throw new RuntimeException("No pom.xml file found");
        }
    }

    /**
     * Saves the gradingResults of the grading to the database and to Canvas if the submission passed
     *
     * @param gradingResults the results of the grading
     */
    private void saveResults(TestAnalyzer.TestNode gradingResults, int numCommits) {
        String headHash = getHeadHash();

        FinalScore finalScore = getFinalAdjustedScore(gradingResults);

        SubmissionDao submissionDao = DaoService.getSubmissionDao();
        Submission submission = new Submission(
                netId,
                repoUrl,
                headHash,
                finalScore.handInDate.toInstant(),
                phase,
                gradingResults.numTestsFailed == 0,
                finalScore.finalScore,
                numCommits,
                getNotes(gradingResults, gradingResults.numTestsFailed == 0, finalScore.daysLate),
                gradingResults
        );

        if (submission.passed()) {
            sendToCanvas(submission);
        }

        submissionDao.insertSubmission(submission);
    }

    private record FinalScore(
            float reportedScore,
            float finalScore,
            ZonedDateTime handInDate,
            ZonedDateTime dueDate,
            int daysLate
    ) {}

    /**
     * Gets the reported score from the grader, and then adds appropriate adjustments.
     * These adjustments include any late penalty that should be applied.
     *
     * @return A result object containing multiple fields.
     * One of those is the `finalScore` that should be saved in the grade-book.
     */
    private FinalScore getFinalAdjustedScore(TestAnalyzer.TestNode gradingResults) {
        // Due Date
        int assignmentNum = PhaseUtils.getPhaseAssignmentNumber(phase);
        int canvasUserId = DaoService.getUserDao().getUser(netId).canvasUserId();
        ZonedDateTime dueDate = getStudentDueDate(canvasUserId, assignmentNum);

        // Hand In Date
        ZonedDateTime handInDate = getSubmissionHandInDate();

        // Scoring
        float reportedScore = getScore(gradingResults);
        int daysLate = getDaysLate(dueDate, handInDate);
        float finalScore = applyGradePenalties(reportedScore, daysLate);

        // Return
        return new FinalScore(
                reportedScore,
                finalScore,
                handInDate,
                dueDate,
                daysLate
        );
    }
    private ZonedDateTime getStudentDueDate(int canvasUserId, int assignmentNum) {
        try {
            return CanvasIntegration.getAssignmentDueDateForStudent(canvasUserId, assignmentNum);
        } catch (CanvasException e) {
            throw new RuntimeException("Failed to get due date for assignment " + assignmentNum + " for user " + netId, e);
        }
    }
    private ZonedDateTime getSubmissionHandInDate() {
        return DaoService.getQueueDao().get(netId).timeAdded().atZone(ZoneId.of("America/Denver"));
    }
    private int getDaysLate(ZonedDateTime dueDate, ZonedDateTime handInDate) {
        int actualDaysLate = DateTimeUtils.getNumDaysLate(handInDate, dueDate);
        // FIXME! Exclude certain holidays
        return Math.min(gradingSettings.MAX_PENALIZE_DAYS_LATE(), actualDaysLate); // Effective days late
    }
    private float applyGradePenalties(float reportedScore, int daysLate) {
        int percentagePenalty = 0;

        // Late penalty: Lose X% per day late
        int latePenaltyPerDay = gradingSettings.LATE_PENALTY_PCT_PER_DAY();
        percentagePenalty += daysLate * latePenaltyPerDay;

        // Apply and return
        float finalScore = reportedScore - (percentagePenalty / 100.0F);
        if (finalScore < 0) finalScore = 0;
        return finalScore;
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
        try (Git git = Git.open(new File(stageRepoPath))) {
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
                .setDirectory(new File(stageRepoPath));

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

        try (Git git = Git.open(new File(stageRepoPath))) {
            Iterable<RevCommit> commits = git.log().all().call();
            Submission submission = DaoService.getSubmissionDao().getFirstPassingSubmission(netId, phase);
            long timestamp = submission == null ? 0L : submission.timestamp().getEpochSecond();
            Map<String, Integer> commitHistory = CommitAnalytics.handleCommits(commits, timestamp, Instant.now().getEpochSecond());
            int numCommits = CommitAnalytics.getTotalCommits(commitHistory);
//            int requiredCommits = gradingSettings.REQUIRED_COMMITS();
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
            processBuilder.directory(new File(stageRepoPath));
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
     * Run the unit tests written by the student
     */
    protected abstract void runCustomTests();

    /**
     * Compiles the test files with the student code
     */
    protected abstract void compileTests();

    /**
     * Runs the tests on the student code
     */
    protected abstract TestAnalyzer.TestNode runTests();

    /**
     * Gets the score for the phase
     *
     * @return the score
     */
    protected abstract float getScore(TestAnalyzer.TestNode results);

    protected abstract String getNotes(TestAnalyzer.TestNode results, boolean passed, int numDaysLate);

    public interface Observer {
        void notifyStarted();

        void update(String message);

        void notifyError(String message);

        void notifyDone(TestAnalyzer.TestNode results);
    }

}
