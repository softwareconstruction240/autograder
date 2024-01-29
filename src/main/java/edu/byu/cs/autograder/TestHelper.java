package edu.byu.cs.autograder;

import java.io.*;
import java.nio.file.Path;
import java.util.Set;

public class TestHelper {

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

        try {
            Process process = processBuilder.start();

            if (process.waitFor() != 0) {
//                throw new RuntimeException("exited with non-zero exit code");
            }

            String output = getOutputFromProcess(process);

            TestAnalyzer testAnalyzer = new TestAnalyzer();

            return testAnalyzer.parse(output.split("\n"), extraCreditTests);

        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

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
}
