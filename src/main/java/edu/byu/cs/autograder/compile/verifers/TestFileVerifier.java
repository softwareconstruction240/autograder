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

/**
 * Verifies the packages used for testing. Provides a warning
 * to the observer if the verification fails and scans the directory for any 'unrecognized' packages and notifies the observer
 * of any found files or directories.
 */
public abstract class TestFileVerifier implements StudentCodeVerifier {

    private static final Set<String> STUDENT_UNIT_TEST_PACKAGES = Set.of(
            "passoff",
            "service",
            "dataaccess",
            "client"
    );
    private static final String PATH_TO_TEST_JAVA_FROM_MODULE = "/src/test/java";
    protected final Set<String> foundFiles = new TreeSet<>();
    private final Set<String> visitedModules = new HashSet<>();
    private GradingContext context;
    private StudentCodeReader reader;

    /**
     * Verifies the student's custom test.
     * @param context A grading context
     * @param reader A student code reader
     * @throws GradingException if autograder cannot derive package from file or could not read file when
     * scanning for package statement
     */
    @Override
    public void verify(GradingContext context, StudentCodeReader reader) throws GradingException {
        this.context = context;
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
                    verifyPackage(packageDirectory);
                }
            }
            currPhase = PhaseUtils.getPreviousPhase(currPhase);
        } while (currPhase != null);

        String message = buildMessage();
        if (!message.isBlank()) {
            context.observer().notifyWarning(message);
        }

        foundFiles.clear();
        visitedModules.clear();
        clearSets();
    }

    protected abstract void clearSets();

    /**
     * Check that the provided test directory contains any unexpected test packages
     * (not one of client, service, dataaccess, passoff) as specified above. If there
     * is an unrecognized package, add it to a list and verify the unrecognized package.
     * @param testJavaDirectory File to a test/java directory under a given module
     */
    private void checkForUnrecognizedPackages(File testJavaDirectory) throws GradingException {
        Set<String> unrecognizedPackages = new HashSet<>();
        for (File childPackage : FileUtils.getChildren(testJavaDirectory, 1)) {
            if (childPackage.isDirectory() && !STUDENT_UNIT_TEST_PACKAGES.contains(childPackage.getName())) {
                unrecognizedPackages.add(childPackage.getAbsolutePath());
            }
        }
        for (String unrecognizedPackage : unrecognizedPackages) {
            Path unrecognizedPackagePath = Path.of(unrecognizedPackage);
            foundFiles.add(stripOffContextRepo(unrecognizedPackagePath));
            File unrecognizedPackageFile = unrecognizedPackagePath.toFile();
            verifyPackage(unrecognizedPackageFile);
            String regex = unrecognizedPackage + ".*\\.java";
            for (File childFile : reader.filesMatching(regex).toList()) {
                foundFiles.add(stripOffContextRepo(childFile.toPath()));
            }
        }
    }

    /**
     * Verify that the package's files within it fulfill the verification requirement.
     *
     * @param packageDirectory File of a package
     */
    private void verifyPackage(File packageDirectory) throws GradingException {
        String regex = packageDirectory.getAbsolutePath() + ".+\\.java";
        for (File file : reader.filesMatching(regex).toList()) {
            if (file.isDirectory()) continue;
            verifyPackageFile(file);
        }
    }

    /**
     * Verify that the file fulfills the verification requirement.
     *
     * @param file A file
     */
    protected abstract void verifyPackageFile(File file) throws GradingException;

    /**
     * Strips off the context repo from that path.
     *
     * @param path Example: "IdeaProjects/autograder/tmp/src/server/.../Server.java
     * @return src/server/.../Server.java
     */
    private String stripOffContextRepo(Path path) {
        Path strippedChildPath = path.subpath(context.stageRepo().toPath().getNameCount(), path.getNameCount());
        return strippedChildPath.toString();
    }

    /**
     * @return the message displayed to the observer.
     */
    protected abstract String buildMessage();

}
