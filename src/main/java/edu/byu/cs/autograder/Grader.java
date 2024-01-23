package edu.byu.cs.autograder;

import edu.byu.cs.canvas.CanvasException;
import edu.byu.cs.canvas.CanvasIntegration;
import edu.byu.cs.dataAccess.DaoService;
import edu.byu.cs.dataAccess.PhaseConfigurationDao;
import edu.byu.cs.dataAccess.SubmissionDao;
import edu.byu.cs.dataAccess.UserDao;
import edu.byu.cs.model.Phase;
import edu.byu.cs.model.PhaseConfiguration;
import edu.byu.cs.model.Submission;
import edu.byu.cs.model.User;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.Date;
import java.util.Set;
import java.util.stream.Stream;

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

    protected Observer observer;

    // FIXME: dynamically get assignment numbers
    private final int PHASE0_ASSIGNMENT_NUMBER = 880445;
    private final int PHASE1_ASSIGNMENT_NUMBER = 880446;
    private final int PHASE3_ASSIGNMENT_NUMBER = 880448;
    private final int PHASE4_ASSIGNMENT_NUMBER = 880449;
    private final int PHASE6_ASSIGNMENT_NUMBER = 880451;

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

        this.observer = observer;
    }

    public void run() {
        observer.notifyStarted();

        try {
            fetchRepo();
            verifyProjectStructure();
            runCustomTests();
            packageRepo();
            compileTests();
            TestAnalyzer.TestNode results = runTests();
            saveResults(results);
            observer.notifyDone(results);

        } catch (Exception e) {
            observer.notifyError(e.getMessage());

            LOGGER.error("Error running grader for user " + netId + " and repository " + repoUrl, e);
        } finally {
            removeStage();
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
     * Saves the results of the grading to the database and to Canvas if the submission passed
     *
     * @param results the results of the grading
     */
    private void saveResults(TestAnalyzer.TestNode results) {
        String headHash = getHeadHash();

        int assignmentNum = switch (phase) {
            case Phase0 -> PHASE0_ASSIGNMENT_NUMBER;
            case Phase1 -> PHASE1_ASSIGNMENT_NUMBER;
            case Phase3 -> PHASE3_ASSIGNMENT_NUMBER;
            case Phase4 -> PHASE4_ASSIGNMENT_NUMBER;
            case Phase6 -> PHASE6_ASSIGNMENT_NUMBER;
        };

        int canvasUserId = DaoService.getUserDao().getUser(netId).canvasUserId();

        ZonedDateTime dueDate;
        try {
            dueDate = CanvasIntegration.getAssignmentDueDateForStudent(canvasUserId, assignmentNum);
        } catch (CanvasException e) {
            throw new RuntimeException("Failed to get due date for assignment " + assignmentNum + " for user " + netId, e);
        }

        // penalize at most 5 days
        int numDaysLate = Math.min(getNumDaysLate(dueDate), 5);
        float score = getScore(results);
        score -= numDaysLate * 0.1F;

        SubmissionDao submissionDao = DaoService.getSubmissionDao();
        Submission submission = new Submission(
                netId,
                repoUrl,
                headHash,
                Instant.now(),
                phase,
                results.numTestsFailed == 0,
                score,
                getNotes(results),
                results
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

        int assignmentNum = switch (phase) {
            case Phase0 -> PHASE0_ASSIGNMENT_NUMBER;
            case Phase1 -> PHASE1_ASSIGNMENT_NUMBER;
            case Phase3 -> PHASE3_ASSIGNMENT_NUMBER;
            case Phase4 -> PHASE4_ASSIGNMENT_NUMBER;
            case Phase6 -> PHASE6_ASSIGNMENT_NUMBER ;
        };

        //FIXME
        float score = submission.score() * switch(phase) {
            case Phase0, Phase1, Phase4 -> 125.0F;
            case Phase3 -> 180.0F;
            case Phase6 -> 155.0F;
        };

        try {
            CanvasIntegration.submitGrade(userId, assignmentNum, score);
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
     * Removes the stage directory if it exists
     */
    private void removeStage() {
        File file = new File(stagePath);

        if (!file.exists()) {
            return;
        }

        try (Stream<Path> paths = Files.walk(file.toPath())) {
            paths.sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        } catch (IOException e) {
            LOGGER.error("Failed to delete stage directory", e);
            throw new RuntimeException("Failed to delete stage directory: " + e.getMessage());
        }
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

    protected abstract String getNotes(TestAnalyzer.TestNode results);

    /**
     * Gets the number of days late the submission is. This excludes weekends and public holidays
     *
     * @param dueDate the due date of the phase
     * @return the number of days late or 0 if the submission is not late
     */
    private int getNumDaysLate(ZonedDateTime dueDate) {
        // end of day
        dueDate = dueDate.withHour(23).withMinute(59).withSecond(59);

        ZonedDateTime now = ZonedDateTime.now();
        int daysLate = 0;

        while (now.isAfter(dueDate)) {
            if (now.getDayOfWeek().getValue() < 6 && !isPublicHoliday(now)) {
                daysLate++;
            }
            now = now.minusDays(1);
        }

        return daysLate;
    }

    /**
     * Checks if the given date is a public holiday
     *
     * @param zonedDateTime the date to check
     * @return true if the date is a public holiday, false otherwise
     */
    private boolean isPublicHoliday(ZonedDateTime zonedDateTime) {
        Date date = Date.from(zonedDateTime.toInstant());
        // TODO: use non-hardcoded list of public holidays
        Set<Date> publicHolidays = Set.of(
                Date.from(ZonedDateTime.parse("2023-02-19T00:00:00.000Z").toInstant()),
                Date.from(ZonedDateTime.parse("2023-03-15T00:00:00.000Z").toInstant())
        );

        return publicHolidays.contains(date);
    }

    public interface Observer {
        void notifyStarted();

        void update(String message);

        void notifyError(String message);

        void notifyDone(TestAnalyzer.TestNode results);
    }

}
