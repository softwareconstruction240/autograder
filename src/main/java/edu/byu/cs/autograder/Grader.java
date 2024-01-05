package edu.byu.cs.autograder;

import edu.byu.cs.dataAccess.DaoService;
import edu.byu.cs.dataAccess.SubmissionDao;
import edu.byu.cs.model.Phase;
import edu.byu.cs.model.Submission;
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
import java.util.Comparator;
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

    /**
     * Creates a new grader
     *
     * @param repoUrl  the url of the student repo
     * @param netId    the netId of the student
     * @param observer the observer to notify of updates
     * @param phase   the phase to grade
     */
    public Grader(String repoUrl, String netId, Observer observer, Phase phase) throws IOException {
        this.netId = netId;
        this.phase = phase;
        this.phasesPath = new File("./phases").getCanonicalPath();
        this.libsDir = new File(phasesPath, "libs").getCanonicalPath();
        this.standaloneJunitJarPath = new File(libsDir, "junit-platform-console-standalone-1.10.1.jar").getCanonicalPath();
        this.junitJupiterApiJarPath = new File(libsDir, "junit-jupiter-api-5.10.1.jar").getCanonicalPath();

        this.stagePath = new File("./tmp-" + repoUrl.hashCode()  + "-" + Instant.now().getEpochSecond()).getCanonicalPath();

        this.repoUrl = repoUrl;
        this.stageRepoPath = new File(stagePath, "repo").getCanonicalPath();

        this.observer = observer;
    }

    public void run() {
        observer.notifyStarted();

        try {
            fetchRepo();
            runCustomTests();
            packageRepo();
            compileTests();
            TestAnalyzer.TestNode results = runTests();
            saveResults(results);
            observer.notifyDone(results);

        } catch (Exception e) {
            observer.notifyError(e.getMessage());

            //TODO: add user context to error message
            LOGGER.error("Error running grader for "+ repoUrl, e);
        } finally {
            removeStage();
        }
    }

    private void saveResults(TestAnalyzer.TestNode results) {
        String headHash;
        try (Git git = Git.open(new File(stageRepoPath))) {
            headHash = git.getRepository().findRef("HEAD").getObjectId().getName();
        } catch (IOException e) {
            throw new RuntimeException("Failed to get head hash: " + e.getMessage());
        }


        SubmissionDao submissionDao = DaoService.getSubmissionDao();
        Submission submission = new Submission(
                netId,
                repoUrl,
                headHash,
                Instant.now(),
                phase,
                getScore(results),
                results
        );
        submissionDao.insertSubmission(submission);
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
            throw new RuntimeException("Failed to clone repo: " + e.getMessage());
        }

        observer.update("Successfully fetched repo");
    }

    /**
     * Packages the student repo into a jar
     */
    protected void packageRepo() {
        observer.update("Packaging repo...");

        String[] commands = new String[]{"compile", "package"};

        for (String command : commands) {
            observer.update("  Running maven " + command + " command...");
            ProcessBuilder processBuilder = new ProcessBuilder();
            processBuilder.directory(new File(stageRepoPath));
            processBuilder.command("mvn", command);
            try {
//                processBuilder.inheritIO();
                Process process = processBuilder.start();
                if (process.waitFor() != 0) {
                    throw new RuntimeException("Unable to " + command + " repo");
                }
            } catch (IOException | InterruptedException ex) {
                throw new RuntimeException("Failed to package repo: " + ex.getMessage());
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
     * @return the score
     */
    protected abstract float getScore(TestAnalyzer.TestNode results);

    public interface Observer {
        void notifyStarted();
        void update(String message);
        void notifyError(String message);
        void notifyDone(TestAnalyzer.TestNode results);
    }

}
