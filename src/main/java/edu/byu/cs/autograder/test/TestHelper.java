package edu.byu.cs.autograder.test;

import edu.byu.cs.autograder.GradingException;
import edu.byu.cs.model.Rubric;
import edu.byu.cs.util.FileUtils;
import edu.byu.cs.util.ProcessUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

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
    void compileTests(File stageRepoPath, String module, File testsLocation, String stagePath, Set<String> excludedTests)
            throws GradingException {
        if(!testsLocation.exists()) return;
        // remove any existing tests
        FileUtils.removeDirectory(new File(stagePath + "/tests"));

        // absolute path to student's chess jar

        try {

            /* Find files to compile */
            List<String> findCommands = getFindCommands(excludedTests);

            ProcessBuilder findProcessBuilder = new ProcessBuilder()
                    .directory(testsLocation)
                    .command(findCommands);

            String findOutput = ProcessUtils.runProcess(findProcessBuilder).stdOut().replace("\n", " ");

            /* Compile files */
            String chessJarWithDeps;
            chessJarWithDeps = new File(stageRepoPath, "/" + module + "/target/" + module + "-jar-with-dependencies.jar")
                    .getCanonicalPath();

            String sharedJarWithDeps = new File(stageRepoPath, "/shared/target/shared-jar-with-dependencies.jar")
                    .getCanonicalPath();

            List<String> compileCommands = getCompileCommands(stagePath, chessJarWithDeps + ":" + sharedJarWithDeps);

            ProcessBuilder compileProcessBuilder =
                    new ProcessBuilder()
                            .directory(testsLocation)
                            .command(compileCommands);

            ProcessUtils.ProcessOutput compileOutput = ProcessUtils.runProcess(compileProcessBuilder, findOutput);

            if (compileOutput.statusCode() != 0) {
                LOGGER.error("Error compiling tests: " + compileOutput.stdErr());
                throw new GradingException("Error compiling tests:", compileOutput.stdErr());
            }
        } catch (IOException | ProcessUtils.ProcessException e) {
            LOGGER.error("Error compiling tests", e);
            throw new GradingException("Error compiling tests", e);
        }
    }

    private static List<String> getFindCommands(Set<String> excludedTests) {
        List<String> commands = new ArrayList<>();
        commands.add("find");
        commands.add(".");
        commands.add("-name");
        commands.add("*.java");

        return commands;
    }

    private static List<String> getCompileCommands(String stagePath, String chessJarWithDeps) {
        List<String> commands = new ArrayList<>();
        commands.add("xargs");
        commands.add("javac");
        commands.add("-d");
        commands.add(stagePath + "/tests");
        commands.add("-cp");
        commands.add(".:" + chessJarWithDeps + ":" + standaloneJunitJarPath + ":" + junitJupiterApiJarPath + ":" + passoffDependenciesPath);
        return commands;
    }

    /**
     * Runs the JUnit tests in the given directory
     *
     * @param uberJar          The jar file containing the compiled classes to be tested.
     * @param compiledTests    The directory containing the compiled test classes.
     * @param packagesToTest   A set of packages to test. Example: {"package1", "package2"}
     * @param extraCreditTests A set of extra credit tests. Example: {"ExtraCreditTest1", "ExtraCreditTest2"}
     * @return A TestNode object containing the results of the tests.
     */
    TestAnalyzer.TestAnalysis runJUnitTests(File uberJar, File compiledTests, Set<String> packagesToTest,
                                     Set<String> extraCreditTests) throws GradingException {
        // Process cannot handle relative paths or wildcards,
        // so we need to only use absolute paths and find
        // to get the files

        String uberJarPath = uberJar.getAbsolutePath();

        List<String> commands = getRunCommands(packagesToTest, uberJarPath);

        ProcessBuilder processBuilder = new ProcessBuilder()
                .directory(compiledTests)
                .command(commands);

        try {
            ProcessUtils.ProcessOutput processOutput = ProcessUtils.runProcess(processBuilder);
            String output = processOutput.stdOut();
            String error = processOutput.stdErr();

            TestAnalyzer testAnalyzer = new TestAnalyzer();
            return testAnalyzer.parse(output.split("\n"), extraCreditTests, removeSparkLines(error));
        } catch (ProcessUtils.ProcessException e) {
            LOGGER.error("Error running tests", e);
            throw new GradingException("Error running tests", e);
        }
    }

    private static List<String> getRunCommands(Set<String> packagesToTest, String uberJarPath) {
        List<String> commands = new ArrayList<>();
        commands.add("java");
        commands.add("-jar");
        commands.add(standaloneJunitJarPath);
        commands.add("execute");
        commands.add("--class-path");
        commands.add(".:" + uberJarPath + ":" + junitJupiterApiJarPath + ":" + passoffDependenciesPath);
        commands.add("--details=testfeed");

        for (String packageToTest : packagesToTest) {
            commands.add("-p");
            commands.add(packageToTest);
        }
        return commands;
    }


    /**
     * Gets the names of all the test files in the phases directory. The expected use case is to
     * us this list to build the excludedTests set for the compileTests method.
     *
     * @return A set of the names of all the test files in the phases directory
     */
    Set<String> getTestFileNames(File testDirectory) throws GradingException {
        Set<String> testFileNames = new HashSet<>();
        try {
            Path testDirectoryPath = Path.of(testDirectory.getCanonicalPath());
            try(Stream<Path> stream = Files.walk(testDirectoryPath)) {
                stream.filter(Files::isRegularFile).forEach(path -> {
                    String fileName = path.getFileName().toString();
                    if (fileName.endsWith(".java")) {
                        testFileNames.add(fileName);
                    }
                });
            }
        } catch (IOException e) {
            throw new GradingException(e);
        }
        return testFileNames;
    }

    public static boolean checkIfPassedPassoffTests(Rubric rubric) {
        boolean passed = true;

        if (rubric.passoffTests() != null && rubric.passoffTests().results() != null)
            if (rubric.passoffTests().results().score() < rubric.passoffTests().results().possiblePoints())
                passed = false;

        return passed;
    }

    private static String removeSparkLines(String errorOutput) {
        List<String> lines = new ArrayList<>(Arrays.asList(errorOutput.split("\n")));
        lines.removeIf(s -> s.matches("^\\[(main|Thread-\\d*)] INFO.*$"));
        return String.join("\n", lines);
    }
}
