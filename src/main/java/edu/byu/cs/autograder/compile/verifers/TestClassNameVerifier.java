package edu.byu.cs.autograder.compile.verifers;

import edu.byu.cs.autograder.GradingContext;
import edu.byu.cs.autograder.GradingException;
import edu.byu.cs.autograder.compile.StudentCodeReader;
import edu.byu.cs.autograder.compile.StudentCodeVerifier;
import edu.byu.cs.model.Phase;
import edu.byu.cs.util.FileUtils;
import edu.byu.cs.util.PhaseUtils;

import java.io.File;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Verifies that the unit test files are named using the expected pattern. Provides a warning
 * to the observer if not and scans the directory for any 'unrecognized' files and notifies the observer
 * of any found files.
 */
public class TestClassNameVerifier implements StudentCodeVerifier {

    private static final Set<String> STUDENT_UNIT_TEST_PACKAGES = Set.of(
            "passoff",
            "service",
            "dataaccess",
            "client"
    );
    private static final String PATH_TO_TEST_JAVA_FROM_MODULE = "/src/test/java";
    private final Set<String> incorrectFileNames = new TreeSet<>();
    private final Set<String> visitedModules = new HashSet<>();
    private StudentCodeReader reader;

    /**
     * Verifies the student's custom test by checking that they have the correct class name.
     * @param context A grading context
     * @param reader A student code reader
     */
    @Override
    public void verify(GradingContext context, StudentCodeReader reader) throws GradingException {
        this.reader = reader;

        Phase currPhase = context.phase();
        do {
            for (String unitTestPackagePath : PhaseUtils.requiredTestPackagePaths(currPhase)) {
                File packageDirectory = new File(context.stageRepo(), unitTestPackagePath);
                if (!packageDirectory.isDirectory()) {
                    String module = PhaseUtils.getModuleUnderTest(currPhase);
                    File testJavaDirectory = new File(context.stageRepo(), module + PATH_TO_TEST_JAVA_FROM_MODULE);
                    // check if the student potentially misspelled a package name.
                    if (testJavaDirectory.isDirectory() && !visitedModules.contains(module)) {
                        checkForUnrecognizedPackages(testJavaDirectory);
                        visitedModules.add(module);
                    }
                } else {
                    verifyPackageNaming(packageDirectory);
                }
            }
            currPhase = PhaseUtils.getPreviousPhase(currPhase);
        } while (currPhase != null);

        String message = buildMessage();
        if (!message.isBlank()) {
            context.observer().notifyWarning(message);
        }

        incorrectFileNames.clear();
        visitedModules.clear();
    }

    /**
     * Check that the provided test directory contains any unexpected test packages
     * (not one of client, service, dataaccess, passoff) as specified above. If there
     * is an unrecognized package, add it to a list and verify the unrecognized package.
     * @param testJavaDirectory File to a test/java directory under a given module
     */
    private void checkForUnrecognizedPackages(File testJavaDirectory) {
        Set<String> unrecognizedPackages = new HashSet<>();
        for (File childPackage : FileUtils.getChildren(testJavaDirectory, 1)) {
            if (childPackage.isDirectory() && !STUDENT_UNIT_TEST_PACKAGES.contains(childPackage.getName())) {
                unrecognizedPackages.add(childPackage.getAbsolutePath());
            }
        }
        for (String unrecognizedPackage : unrecognizedPackages) {
            Path unrecognizedPackagePath = Path.of(unrecognizedPackage);
            File unrecognizedPackageFile = unrecognizedPackagePath.toFile();
            verifyPackageNaming(unrecognizedPackageFile);
        }
    }

    /**
     * Verify that the package's files within it contain are correctly
     * named according to the required pattern.
     *
     * @param packageDirectory File of a package
     */
    private void verifyPackageNaming(File packageDirectory) {
        String regex = packageDirectory.getAbsolutePath() + ".+\\.java";
        for (File file : reader.filesMatching(regex).toList()) {
            if (file.isDirectory()) continue;
            fileCorrectlyNamed(file);
        }
    }

    /**
     * Verify that the file's name matches the required pattern.
     *
     * @param file A file
     */
    private void fileCorrectlyNamed(File file) {
        Pattern pattern = Pattern.compile("^(?:.+Tests?|Test[A-Z].+)\\.java$");
        Matcher matcher = pattern.matcher(file.getName());
        if (!matcher.find()) {
            incorrectFileNames.add(file.getName());
        }
    }

    /**
     * @return the message displayed to the observer.
     */
    private String buildMessage() {
        StringBuilder stringBuilder = new StringBuilder();
        if (!incorrectFileNames.isEmpty()) {
            stringBuilder.append("File(s) with incorrect file name: ")
                    .append(String.join(", ", incorrectFileNames))
                    .append(".\n");
        }
        if (!stringBuilder.isEmpty()) {
            stringBuilder.append("You may need to reread the phase's specifications for proper project structure.\n")
                    .append("Make sure the test files are named correctly.");
        }
        return stringBuilder.toString();
    }
}
