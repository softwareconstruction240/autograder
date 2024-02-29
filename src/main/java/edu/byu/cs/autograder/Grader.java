package edu.byu.cs.autograder;

import edu.byu.cs.analytics.CommitAnalytics;
import edu.byu.cs.canvas.CanvasException;
import edu.byu.cs.canvas.CanvasIntegration;
import edu.byu.cs.canvas.CanvasUtils;
import edu.byu.cs.controller.SubmissionController;
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
import java.util.HashMap;
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

    /**
     * The max number of days that the late penalty should be applied for.
     */
    private final int MAX_LATE_DAYS_TO_PENALIZE = 5;

    /**
     * The penalty to be applied per day to a late submission.
     * This is out of 1. So putting 0.1 would be a 10% deduction per day
     */
    private final float PER_DAY_LATE_PENALTY = 0.1F;

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

            RubricConfig rubricConfig = DaoService.getRubricConfigDao().getRubricConfig(phase);
            Rubric.Results qualityResults = null;
            if(rubricConfig.quality() != null) {
                qualityResults = runQualityChecks();
            }

            compileTests();
            Rubric.Results passoffResults = null;
            if(rubricConfig.passoffTests() != null) {
                passoffResults = runTests();
            }
            Rubric.Results customTestsResults = null;
            if(rubricConfig.unitTests() != null) {
                customTestsResults = runCustomTests();
            }

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
            rubric = CanvasUtils.decimalScoreToPoints(phase, rubric);
            rubric = annotateRubric(rubric);

            int daysLate = calculateLateDays(rubric);
            float thisScore = calculateScoreWithLatePenalty(rubric, daysLate);

            Submission thisSubmission;
            float highestScore = getCanvasScore();

            // prevent score from being saved to canvas if it will lower their score
            if (thisScore <= highestScore) {
                String notes = "Submission did not improve current score. (" + (highestScore * 100) + "%) Score not saved to Canvas.\n";
                thisSubmission = saveResults(rubric, numCommits,daysLate, thisScore, notes);
            } else {
                thisSubmission = saveResults(rubric, numCommits, daysLate, thisScore, "");
                sendToCanvas(thisSubmission, 1 - (daysLate * PER_DAY_LATE_PENALTY));
            }

            observer.notifyDone(thisSubmission);

        } catch (Exception e) {
            observer.notifyError(e.getMessage());

            LOGGER.error("Error running grader for user " + netId + " and repository " + repoUrl, e);
        } finally {
            FileUtils.removeDirectory(new File(stagePath));
        }
    }

    /**
     * gets the score stored in canvas for the current user and phase
     * @return score. returns 1.0 for a score of 100%. returns 0.5 for a score of 50%.
     */
    private float getCanvasScore() {
        User user = DaoService.getUserDao().getUser(netId);

        int userId = user.canvasUserId();

        int assignmentNum = PhaseUtils.getPhaseAssignmentNumber(phase);
        try {
            CanvasIntegration.CanvasSubmission submission = CanvasIntegration.getSubmission(userId, assignmentNum);
            int totalPossiblePoints = DaoService.getRubricConfigDao().getPhaseTotalPossiblePoints(phase);
            return submission.score() / totalPossiblePoints;
        } catch (CanvasException e) {
            throw new RuntimeException(e);
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

    private int calculateLateDays(Rubric rubric) {
        int assignmentNum = PhaseUtils.getPhaseAssignmentNumber(phase);

        int canvasUserId = DaoService.getUserDao().getUser(netId).canvasUserId();

        ZonedDateTime dueDate;
        try {
            dueDate = CanvasIntegration.getAssignmentDueDateForStudent(canvasUserId, assignmentNum);
        } catch (CanvasException e) {
            throw new RuntimeException("Failed to get due date for assignment " + assignmentNum + " for user " + netId, e);
        }

        ZonedDateTime handInDate = DaoService.getQueueDao().get(netId).timeAdded().atZone(ZoneId.of("America/Denver"));
        return Math.min(DateTimeUtils.getNumDaysLate(handInDate, dueDate), MAX_LATE_DAYS_TO_PENALIZE);
    }

    private float calculateScoreWithLatePenalty(Rubric rubric, int numDaysLate) {
        float score = getScore(rubric);
        score -= numDaysLate * PER_DAY_LATE_PENALTY;
        if (score < 0) score = 0;
        return score;
    }

    /**
     * Saves the results of the grading to the database if the submission passed
     *
     * @param rubric the rubric for the phase
     */
    private Submission saveResults(Rubric rubric, int numCommits, int numDaysLate, float score, String notes) {
        String headHash = getHeadHash();

        if (numDaysLate > 0)
            notes += numDaysLate + " days late. -" + (numDaysLate * 10) + "%";

        // FIXME: this is code duplication from calculateLateDays()
        ZonedDateTime handInDate = DaoService.getQueueDao().get(netId).timeAdded().atZone(ZoneId.of("America/Denver"));

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
                notes,
                rubric
        );

        submissionDao.insertSubmission(submission);
        return submission;
    }

    private void sendToCanvas(Submission submission, float lateAdjustment) {
        UserDao userDao = DaoService.getUserDao();
        User user = userDao.getUser(netId);

        int userId = user.canvasUserId();

        int assignmentNum = PhaseUtils.getPhaseAssignmentNumber(phase);

        RubricConfig rubricConfig = DaoService.getRubricConfigDao().getRubricConfig(phase);
        Map<String, Float> scores = new HashMap<>();
        Map<String, String> comments = new HashMap<>();
        if(rubricConfig.passoffTests() != null) {
            String id = getCanvasRubricId(Rubric.RubricType.PASSOFF_TESTS);
            Rubric.Results results = submission.rubric().passoffTests().results();
            scores.put(id, results.score() * lateAdjustment);
            comments.put(id, results.notes());
        }
        if(rubricConfig.unitTests() != null) {
            String id = getCanvasRubricId(Rubric.RubricType.UNIT_TESTS);
            Rubric.Results results = submission.rubric().unitTests().results();
            scores.put(id, results.score() * lateAdjustment);
            comments.put(id, results.notes());
        }
        if(rubricConfig.quality() != null) {
            String id = getCanvasRubricId(Rubric.RubricType.QUALITY);
            Rubric.Results results = submission.rubric().quality().results();
            scores.put(id, results.score() * lateAdjustment);
            comments.put(id, results.notes());
        }

        try {
            CanvasIntegration.submitGrade(userId, assignmentNum, scores, comments, submission.notes());
        } catch (CanvasException e) {
            LOGGER.error("Error submitting to canvas for user " + submission.netId(), e);
            throw new RuntimeException("Error contacting canvas to record scores");
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
            processBuilder.command("mvn", command, "-DskipTests");
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

    /**
     * Annotates the rubric with notes and passed status
     *
     * @param rubric the rubric to annotate
     * @return the annotated rubric
     */
    protected abstract Rubric annotateRubric(Rubric rubric);

    protected abstract String getCanvasRubricId(Rubric.RubricType type);

    public interface Observer {
        void notifyStarted();

        void update(String message);

        void notifyError(String message);

        void notifyDone(Submission submission);
    }

}
