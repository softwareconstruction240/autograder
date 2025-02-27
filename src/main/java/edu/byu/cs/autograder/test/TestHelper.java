package edu.byu.cs.autograder.test;

import edu.byu.cs.autograder.GradingException;
import edu.byu.cs.dataAccess.DaoService;
import edu.byu.cs.dataAccess.DataAccessException;
import edu.byu.cs.dataAccess.daoInterface.ConfigurationDao;
import edu.byu.cs.model.Rubric;
import edu.byu.cs.model.TestAnalysis;
import edu.byu.cs.util.FileUtils;
import edu.byu.cs.util.ProcessUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

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
     * Constant value for trimming error outputs
     */
    private int MAX_ERROR_OUTPUT_CHARS;

    public TestHelper() {
        refreshConfigValues();
    }

    private void refreshConfigValues() {
        MAX_ERROR_OUTPUT_CHARS = 10000;
        ConfigurationDao configurationDao = DaoService.getConfigurationDao();
        try {
            Integer maxErrorOutputChars = configurationDao.getConfiguration(ConfigurationDao.Configuration.MAX_ERROR_OUTPUT_CHARS, Integer.class);
            if (maxErrorOutputChars > 0) {
                MAX_ERROR_OUTPUT_CHARS = maxErrorOutputChars;
            }
        } catch (DataAccessException e) {
            // Swallow this error. We don't want this file to ever fail while reading this config value.
            // The value isn't important.
        }
    }


    static {
        Path libsPath = new File("phases", "libs").toPath();
        try {
            standaloneJunitJarPath = new File(libsPath.toFile(), "junit-platform-console-standalone-1.10.1.jar").getCanonicalPath();
            junitJupiterApiJarPath = new File(libsPath.toFile(), "junit-jupiter-api-5.10.1.jar").getCanonicalPath();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Compiles the tests in the given directory
     *
     * @param stageRepoPath     The path to the student's repository
     * @param module            The module to compile
     * @param testsLocations    The location of the tests
     * @param stagePath         The path to the stage directory
     */
    void compileTests(File stageRepoPath, String module, Set<File> testsLocations, String stagePath)
            throws GradingException {
        // remove any existing tests
        FileUtils.removeDirectory(new File(stagePath + "/tests"));

        try {
            for(File testsLocation : testsLocations) {
                if (!testsLocation.exists()) continue;
                /* Find files to compile */
                List<String> findCommands = getFindCommands();

                ProcessBuilder findProcessBuilder = new ProcessBuilder()
                        .directory(testsLocation)
                        .command(findCommands);

                String findOutput = ProcessUtils.runProcess(findProcessBuilder).stdOut().replace("\n", " ");

                /* Compile files */
                String chessJarWithDeps = new File(stageRepoPath, "/" + module + "/target/" + module + "-test-dependencies.jar")
                        .getCanonicalPath();

                List<String> compileCommands = getCompileCommands(stagePath, chessJarWithDeps);

                ProcessBuilder compileProcessBuilder =
                        new ProcessBuilder()
                                .directory(testsLocation)
                                .command(compileCommands);

                ProcessUtils.ProcessOutput compileOutput = ProcessUtils.runProcess(compileProcessBuilder, findOutput);


                if (compileOutput.statusCode() != 0) {
                    LOGGER.error("Error compiling tests: {}", compileOutput.stdErr());
                    Rubric.Results results = Rubric.Results.textError("Error compiling tests", compileOutput.stdErr());
                    throw new GradingException(results.notes(), results);
                }
            }
        } catch (IOException | ProcessUtils.ProcessException e) {
            LOGGER.error("Error compiling tests", e);
            throw new GradingException("Error compiling tests", e);
        }
    }

    private static List<String> getFindCommands() {
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
        commands.add(".:" + chessJarWithDeps + ":" + standaloneJunitJarPath + ":" + junitJupiterApiJarPath);
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
    TestAnalysis runJUnitTests(File uberJar, File compiledTests, Set<String> packagesToTest,
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
            String error = processOutput.stdErr();

            TestAnalyzer testAnalyzer = new TestAnalyzer();
            File testOutputDirectory = new File(compiledTests, "test-output");
            File junitXmlOutput = new File(testOutputDirectory, "TEST-junit-jupiter.xml");
            return testAnalyzer.parse(junitXmlOutput, extraCreditTests, trimErrorOutput(error));
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
        commands.add(".:" + uberJarPath + ":" + junitJupiterApiJarPath);
        commands.add("--details=none");
        commands.add("--reports-dir=./test-output");

        for (String packageToTest : packagesToTest) {
            commands.add("-p");
            commands.add(packageToTest);
        }
        return commands;
    }

    private static String removeSparkLines(String errorOutput) {
        List<String> lines = new ArrayList<>(Arrays.asList(errorOutput.split("\n")));
        lines.removeIf(s -> s.matches("^\\[(main|Thread-\\d*)] INFO.*$"));
        return String.join("\n", lines);
    }

    private String trimErrorOutput(String errorOutput) {
        errorOutput = removeSparkLines(errorOutput);
        if (errorOutput.length() > MAX_ERROR_OUTPUT_CHARS) {
            errorOutput =  errorOutput.substring(0, MAX_ERROR_OUTPUT_CHARS) + "...\n(Error Output Truncated)";
        }
        return errorOutput;
    }
}
