package edu.byu.cs.autograder;

import java.io.File;
import java.io.IOException;

public class PhaseOneGrader extends Grader {

    private final File phaseTests = new File("./phases/phase1");

    private final File stageTestsPath;

    public PhaseOneGrader(String repoUrl, String stagePath, Observer observer) throws IOException {
        super(repoUrl, stagePath, observer);
        this.stageTestsPath = new File(stagePath + "/tests");
    }

    @Override
    protected void runCustomTests() {
        // no unit tests for this phase
    }

    @Override
    protected void compileTests() {
        observer.update("Compiling tests...");

        // Process cannot handle relative paths or wildcards,
        // so we need to only use absolute paths and find
        // to get the files

        // absolute path to student's chess jar
        String chessJarWithDeps;
        try {
            chessJarWithDeps = new File(stageRepoPath, "/shared/target/shared-jar-with-dependencies.jar")
                    .getCanonicalPath();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        ProcessBuilder processBuilder =
                new ProcessBuilder()
                        .directory(phaseTests)
                        .command("find",
                                "passoffTests",
                                "-name",
                                "*.java",
                                "-exec",
                                "javac",
                                "-d",
                                stagePath + "/tests",
                                "-cp",
                                ".:" + chessJarWithDeps + ":junit-platform-console-standalone-1.10.1.jar:junit-jupiter-api-5.10.1.jar",
                                "{}",
                                ";")
                        .inheritIO();

        try {
            Process process = processBuilder.start();
            if (process.waitFor() != 0) {
                throw new RuntimeException("exited with non-zero exit code");
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }

        observer.update("Successfully compiled tests");
    }

    @Override
    protected void runTests() {
        observer.update("Running tests...");

        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.directory(stageTestsPath);

        // Process cannot handle relative paths or wildcards,
        // so we need to only use absolute paths and find
        // to get the files
        String chessJarWithDeps = new File(stageRepoPath, "shared/target/shared-jar-with-dependencies.jar").getAbsolutePath();
        processBuilder.command("java",
                "-jar",
                "junit-platform-console-standalone-1.10.1.jar",
                "--class-path", ".:" + chessJarWithDeps + ":junit-jupiter-api-5.10.1.jar",
                "--scan-class-path");
        processBuilder.inheritIO();

        try {
            Process process = processBuilder.start();
            int exitCode = process.waitFor();
            assert exitCode == 0;
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }

        observer.update("Successfully ran tests");
    }
}
