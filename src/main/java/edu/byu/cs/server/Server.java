package edu.byu.cs.server;

import java.io.File;
import java.io.IOException;

import static edu.byu.cs.git.RepoCloner.fetchRepo;

public class Server {
    public static void main(String[] args) {
        // clone a repo
        fetchRepo("https://github.com/softwareconstruction240/chess.git", "./chess-submission");

        // Navigate to the repo directory
        File projectDir = new File("./chess-submission");

        // build the jar using mvn compile then mvn package
        runMavenCommands(projectDir, "compile", "package");

        // print results
        //~/.jdks/openjdk-21/bin/javac -cp .:chess-jar-with-dependencies.jar:junit-platform-console-standalone-1.10.1.jar:junit-jupiter-api-5.10.1.jar passoffTests/chessTests/**/*.java
        //~/.jdks/openjdk-21/bin/java -jar junit-platform-console-standalone-1.10.1.jar --class-path .:chess-jar-with-dependencies.jar:junit-jupiter-api-5.10.1.jar --scan-class-path
    }

    private static void runMavenCommands(File projectDir, String... commands) {
        for (String command : commands) {
            ProcessBuilder processBuilder = new ProcessBuilder();
            processBuilder.directory(projectDir);
            processBuilder.command("mvn", command);
            try {
                Process process = processBuilder.start();
                int exitCode = process.waitFor();
                assert exitCode == 0;
            } catch (IOException | InterruptedException ex) {
                ex.printStackTrace();
            }
        }
    }
}