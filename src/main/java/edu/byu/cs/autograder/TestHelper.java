package edu.byu.cs.autograder;

import edu.byu.cs.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

/**
 * A helper class for running common test operations
 */
public class TestHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestHelper.class);

    /**
     * The path to the standalone JUnit jar
     */
    private static final String standaloneJunitJarPath;

    /**
     * The path to the JUnit Jupiter API jar
     */
    private static final String junitJupiterApiJarPath;

    /**
     * The path to the passoff dependencies jar
     */
    private static final String passoffDependenciesPath;

    static {
        Path libsPath = new File("phases", "libs").toPath();
        try {
            standaloneJunitJarPath = new File(libsPath.toFile(), "junit-platform-console-standalone-1.10.1.jar").getCanonicalPath();
            junitJupiterApiJarPath = new File(libsPath.toFile(), "junit-jupiter-api-5.10.1.jar").getCanonicalPath();
            passoffDependenciesPath = new File(libsPath.toFile(), "passoff-dependencies.jar").getCanonicalPath();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Compiles the tests in the given directory
     *
     * @param stageRepoPath The path to the student's repository
     * @param module        The module to compile
     * @param testsLocation The location of the tests
     * @param stagePath     The path to the stage directory
     * @param excludedTests A set of tests to exclude from compilation. Can be directory or file names
     */
    void compileTests(File stageRepoPath, String module, File testsLocation, String stagePath, Set<String> excludedTests) {

        // remove any existing tests
        FileUtils.removeDirectory(new File(stagePath + "/tests"));

        // absolute path to student's chess jar
        String chessJarWithDeps;
        try {
            chessJarWithDeps = new File(stageRepoPath, "/" + module + "/target/" + module + "-jar-with-dependencies.jar")
                    .getCanonicalPath();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        List<String> commands = getCompileCommands(stagePath, excludedTests, chessJarWithDeps);

        ProcessBuilder processBuilder =
                new ProcessBuilder()
                        .directory(testsLocation)
//                        .inheritIO() // TODO: implement better logging
                        .command(commands);

        try {
            Process process = processBuilder.start();
            if (process.waitFor() != 0) {
                throw new RuntimeException("exited with non-zero exit code");
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Error compiling tests", e);
        }
    }

    private static List<String> getCompileCommands(String stagePath, Set<String> excludedTests, String chessJarWithDeps) {
        List<String> commands = new ArrayList<>();
        commands.add("find");
        commands.add(".");
        commands.add("-name");
        commands.add("*.java");

        if (!excludedTests.isEmpty())
            commands.add("-not");

        for (String excludedTest : excludedTests) {
            commands.add("-name");
            commands.add(excludedTest);
        }

        commands.add("-exec");
        commands.add("javac");
        commands.add("-d");
        commands.add(stagePath + "/tests");
        commands.add("-cp");
        commands.add(".:" + chessJarWithDeps + ":" + standaloneJunitJarPath + ":" + junitJupiterApiJarPath + ":" + passoffDependenciesPath);
        commands.add("{}");
        commands.add(";");
        return commands;
    }

    /**
     * Runs the JUnit tests in the given directory
     *
     * @param uberJar          The jar file containing the compiled classes to be tested.
     * @param compiledTests    The directory containing the compiled test classes.
     * @param extraCreditTests A set of extra credit tests. Example: {"ExtraCreditTest1", "ExtraCreditTest2"}
     * @return A TestNode object containing the results of the tests.
     */
    TestAnalyzer.TestNode runJUnitTests(File uberJar, File compiledTests, Set<String> extraCreditTests) {
        // Process cannot handle relative paths or wildcards,
        // so we need to only use absolute paths and find
        // to get the files

        String uberJarPath = uberJar.getAbsolutePath();

        ProcessBuilder processBuilder = new ProcessBuilder()
                .directory(compiledTests)
                .command("java",
                        "-jar",
                        standaloneJunitJarPath,
                        "--class-path",
                        ".:" + uberJarPath + ":" + junitJupiterApiJarPath + ":" + passoffDependenciesPath,
                        "--scan-class-path",
                        "--details=testfeed");

        try (ExecutorService processOutputExecutor = Executors.newSingleThreadExecutor()){

            Process process = processBuilder.start();

            /*
            Grab the output from the process asynchronously. Without this concurrency, if this is computed
            synchronously after the process terminates, the pipe from the process may fill up, causing the process
            writes to block, resulting in the process never finishing. This is usually the result of the tested
            code printing out too many lines to stdout as a means of logging/debugging
             */
            Future<String> processOutputFuture = processOutputExecutor.submit(() -> getOutputFromProcess(process));

            if (!process.waitFor(30000, TimeUnit.MILLISECONDS)) {
                process.destroyForcibly();
                LOGGER.error("Tests took too long to run, come see a TA for more info");
                throw new RuntimeException("Tests took too long to run, come see a TA for more info");
            }

            String output = processOutputFuture.get(1000, TimeUnit.MILLISECONDS);

            TestAnalyzer testAnalyzer = new TestAnalyzer();

            return testAnalyzer.parse(output.split("\n"), extraCreditTests);

        } catch (IOException | InterruptedException | ExecutionException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Extracts the output as a string from a process
     *
     * @param process The process to extract the output from
     * @return The output of the process as a string
     * @throws IOException If an error occurs while reading the output
     */
    private static String getOutputFromProcess(Process process) throws IOException {
        String output;

        InputStream is = process.getInputStream();
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(isr);
        {
            String line;
            StringBuilder sb = new StringBuilder();
            while ((line = br.readLine()) != null) {
                sb.append(line);
                sb.append('\n');
            }

            output = sb.toString();
        }
        return output;
    }

    /**
     * Gets the names of all the test files in the phases directory. The expected use case is to
     * us this list to build the excludedTests set for the compileTests method.
     *
     * @return A set of the names of all the test files in the phases directory
     */
    Set<String> getTestFileNames(File testDirectory) {
        Set<String> testFileNames = new HashSet<>();
        try {
            Path testDirectoryPath = Path.of(testDirectory.getCanonicalPath());
            Files.walk(testDirectoryPath)
                    .filter(Files::isRegularFile)
                    .forEach(path -> {
                        String fileName = path.getFileName().toString();
                        if (fileName.endsWith(".java")) {
                            testFileNames.add(fileName);
                        }
                    });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return testFileNames;
    }
}
