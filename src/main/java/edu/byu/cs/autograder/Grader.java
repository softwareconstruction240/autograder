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
     * The url of the student repo
     */
    private final String repoUrl;

    /**
     * The path for the student repo to be put in and tested
     */
    protected final String stagePath;

    /**
     * The path for the student repo (child of stagePath)
     */
    protected final String studentRepoPath;

    protected Observer observer;

    /**
     * Creates a new grader
     * @param repoUrl the url of the student repo
     * @param stagePath the path for the student repo to be put in and tested
     */
    public Grader(String repoUrl, String stagePath, Observer observer) throws IOException {
        this.repoUrl = repoUrl;
        this.stagePath = new File(stagePath).getCanonicalPath();
        this.studentRepoPath = new File(stagePath, "repo").getCanonicalPath();
        this.observer = observer;
    }

    public void run() {

        removeDirectory();
        fetchRepo(repoUrl);
        packageRepo();
        compileTests();
        runTests();
    }

    /**
     * Removes the stage directory if it exists
     */
    private void removeDirectory() {

        File file = new File(stagePath);

        if (!file.exists()) {
            return;
        }

        try (Stream<Path> paths = Files.walk(file.toPath())) {
            paths.sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Fetches the student repo and puts it in the given local path
     * @param repoUrl the url of the student repo
     */
    private void fetchRepo(String repoUrl) {
        CloneCommand cloneCommand = Git.cloneRepository()
                .setURI(repoUrl)
                .setDirectory(new File(studentRepoPath));

        try (Git git = cloneCommand.call()) {
            System.out.println("Cloned repo to " + git.getRepository().getDirectory());
        } catch (GitAPIException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Packages the student repo into a jar
     */
    protected void packageRepo() {
        String[] commands = new String[]{"compile", "package"};

        for (String command : commands) {
            ProcessBuilder processBuilder = new ProcessBuilder();
            processBuilder.directory(new File(studentRepoPath));
            processBuilder.command("mvn", command);
            try {
                processBuilder.inheritIO();
                Process process = processBuilder.start();
                int exitCode = process.waitFor();
                assert exitCode == 0;
            } catch (IOException | InterruptedException ex) {
                ex.printStackTrace();
            }
        }
    }

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
