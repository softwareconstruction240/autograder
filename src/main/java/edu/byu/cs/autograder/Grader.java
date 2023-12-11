package edu.byu.cs.autograder;

import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

/**
 * A template for fetching, compiling, and running student code
 */
public abstract class Grader {

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
     * @param repoUrl the url of the student repo
     * @param stagePath the path for the student repo to be put in and tested
     */
    public Grader(String repoUrl, String stagePath, Observer observer) throws IOException {
        this.phasesPath = new File("./phases").getCanonicalPath();
        this.libsDir = new File(phasesPath, "libs").getCanonicalPath();
        this.standaloneJunitJarPath = new File(libsDir, "junit-platform-console-standalone-1.10.1.jar").getCanonicalPath();
        this.junitJupiterApiJarPath = new File(libsDir, "junit-jupiter-api-5.10.1.jar").getCanonicalPath();

        this.stagePath = new File("./stage").getCanonicalPath();

        this.repoUrl = repoUrl;
        this.stageRepoPath = new File(stagePath, "repo").getCanonicalPath();

        this.observer = observer;
    }

    public void run() {
        try {
            removeDirectory();
            fetchRepo(repoUrl);
            runCustomTests();
            packageRepo();
            compileTests();
            runTests();
        } catch (Exception e) {
            observer.notifyError(e.getMessage());
        }

        observer.notifySuccess();
    }

    /**
     * Removes the stage directory if it exists
     */
    private void removeDirectory() {
        observer.update("Cleaning stage directory...");

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

        observer.update("Successfully cleaned stage directory");
    }

    /**
     * Fetches the student repo and puts it in the given local path
     * @param repoUrl the url of the student repo
     */
    private void fetchRepo(String repoUrl) {
        observer.update("Fetching repo...");

        CloneCommand cloneCommand = Git.cloneRepository()
                .setURI(repoUrl)
                .setDirectory(new File(stageRepoPath));

        try (Git git = cloneCommand.call()) {
            System.out.println("Cloned repo to " + git.getRepository().getDirectory());
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
            observer.update("  Running maven " + command + " command");
            ProcessBuilder processBuilder = new ProcessBuilder();
            processBuilder.directory(new File(stageRepoPath));
            processBuilder.command("mvn", command);
            try {
                processBuilder.inheritIO();
                Process process = processBuilder.start();
                if (process.waitFor() != 0) {
                    throw new RuntimeException("exited with non-zero exit code");
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
    protected abstract void runTests();

    public interface Observer {
        void update(String message);
        void notifyError(String message);
        void notifySuccess();
    }

}
