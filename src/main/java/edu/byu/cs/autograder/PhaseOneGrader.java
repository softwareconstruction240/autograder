package edu.byu.cs.autograder;

import java.io.File;
import java.io.IOException;

public class PhaseOneGrader extends Grader {

    private final File phaseTests = new File("./phases/phase1");

    public PhaseOneGrader(String repoUrl, String localPath) throws IOException {
        super(repoUrl, localPath);
    }

    @Override
    protected void compileTests() {
        // Process cannot handle relative paths or wildcards,
        // so we need to only use absolute paths and find
        // to get the files

        // absolute path to student's chess jar
        String chessJarWithDeps;
        try {
            chessJarWithDeps = new File(studentRepoPath, "/shared/target/shared-jar-with-dependencies.jar")
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
                                "-cp",
                                ".:" + chessJarWithDeps + ":junit-platform-console-standalone-1.10.1.jar:junit-jupiter-api-5.10.1.jar",
                                "{}",
                                ";")
                        .inheritIO();

        try {
            Process process = processBuilder.start();
            int exitCode = process.waitFor();
            assert exitCode == 0;
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void runTests() {
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.directory(phaseTests);

        // Process cannot handle relative paths or wildcards,
        // so we need to only use absolute paths and find
        // to get the files
        String chessJarWithDeps = new File(studentRepoPath, "shared/target/shared-jar-with-dependencies.jar").getAbsolutePath();
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
    }
}
