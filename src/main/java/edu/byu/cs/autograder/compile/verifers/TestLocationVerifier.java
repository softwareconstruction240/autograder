package edu.byu.cs.autograder.compile.verifers;

import edu.byu.cs.autograder.GradingContext;
import edu.byu.cs.autograder.GradingException;
import edu.byu.cs.autograder.compile.StudentCodeReader;
import edu.byu.cs.autograder.compile.StudentCodeVerifier;
import edu.byu.cs.model.Phase;
import edu.byu.cs.util.FileUtils;
import edu.byu.cs.util.PhaseUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Verifies that the packages used for testing are placed in the correct location. Provides a warning
 * to the observer if not and scans the directory for any 'unrecognized' packages and notifies the observer
 * of any found files or directories.
 */
public class TestLocationVerifier implements StudentCodeVerifier {

    private static final Set<String> STUDENT_UNIT_TEST_PACKAGES = Set.of(
            "passoff",
            "service",
            "dataaccess",
            "client"
    );
    private static final String PATH_TO_TEST_JAVA_FROM_MODULE = "/src/test/java";
    private static final String DIRECTORY_BEFORE_PACKAGES = "java";
    private final Set<String> missingPackages = new TreeSet<>();
    private final Set<String> foundFiles = new TreeSet<>();
    private final Set<String> incorrectPackageNames = new TreeSet<>();
    private final Set<String> filesMissingPackageNames = new TreeSet<>();
    private final Set<String> visitedModules = new HashSet<>();
    private GradingContext context;
    private StudentCodeReader reader;

    /**
     * Verifies the student's custom test by checking that they are in the correct directory
     * and the files have the correct package name.
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
            for (String unitTestPackagePath : PhaseUtils.unitTestPackagePaths(currPhase)) {
                File packageDirectory = new File(context.stageRepo(), unitTestPackagePath);
                if (!packageDirectory.isDirectory()) {
                    missingPackages.add(unitTestPackagePath);
                    String module = PhaseUtils.getModuleUnderTest(currPhase);
                    File testJavaDirectory = new File(context.stageRepo(), module + PATH_TO_TEST_JAVA_FROM_MODULE);
                    // check if the student potentially misspelled a package name.
                    if (testJavaDirectory.isDirectory() && !visitedModules.contains(module)) {
                        checkForUnrecognizedPackages(testJavaDirectory);
                        visitedModules.add(module);
                    }
                } else {
                    verifyPackageDirectory(packageDirectory);
                }
            }
            currPhase = PhaseUtils.getPreviousPhase(currPhase);
        } while (currPhase != null);
        
        String message = buildMessage();
        if (!message.isBlank()) {
            context.observer().notifyWarning(message);
        }

        missingPackages.clear();
        foundFiles.clear();
        incorrectPackageNames.clear();
        filesMissingPackageNames.clear();
        visitedModules.clear();
    }

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
            verifyPackageDirectory(unrecognizedPackageFile);
            String regex = unrecognizedPackage + ".*\\.java";
            for (File childFile : reader.filesMatching(regex).toList()) {
                foundFiles.add(stripOffContextRepo(childFile.toPath()));
            }
        }
    }

    /**
     * Verify that the package's files within it contain the correct
     * package statement at the top of the file.
     *
     * @param packageDirectory File of a package
     */
    private void verifyPackageDirectory(File packageDirectory) throws GradingException {
        String regex = packageDirectory.getAbsolutePath() + ".+\\.java";
        for (File file : reader.filesMatching(regex).toList()) {
            if (file.isDirectory()) continue;
            String expectedPackageName = getPackageFromFilePath(file.toPath());
            fileContainsCorrectPackage(file, expectedPackageName);
        }
    }

    /**
     * Verify that the file contains the specified packageName
     *
     * @param file A file
     * @param packageName A package name
     */
    private void fileContainsCorrectPackage(File file, String packageName) throws GradingException {
        String line;
        Pattern pattern = Pattern.compile("^package (.+);");
        try (BufferedReader reader = Files.newBufferedReader(file.toPath())) {
            while ((line = reader.readLine()) != null) {
                Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                    String filePackageName = matcher.group(1);
                    if (!filePackageName.equals(packageName)) {
                        incorrectPackageNames.add(file.getName());
                    }
                    return;
                }
            }
            filesMissingPackageNames.add(file.getName());
        } catch (IOException e) {
            throw new GradingException(
                    String.format("Could not read file %s: %s", file.getName(), e.getMessage())
            );
        }
    }

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
     * @param filePath Path of the file
     * @return The package given the path. For example,
     * ../server/src/test/java/dataaccess/mysql
     * dataaccess.mysql
     */
    private String getPackageFromFilePath(Path filePath) throws GradingException {
        for (int i = 0; i < filePath.getNameCount(); i++) {
            if (filePath.getName(i).toString().equals(DIRECTORY_BEFORE_PACKAGES)) {
                return filePath.subpath(i + 1, filePath.getNameCount() - 1).toString().replace('/', '.');
            }
        }
        throw new GradingException("Could not derive the package of the following file: " + filePath);
    }

    /**
     * @return the message displayed to the observer.
     */
    private String buildMessage() {
        StringBuilder stringBuilder = new StringBuilder();
        if (!missingPackages.isEmpty()) {
            stringBuilder.append("Missing expected package(s): ")
                    .append(String.join(", ", missingPackages))
                    .append(".\n");
        }
        if (!foundFiles.isEmpty()) {
            stringBuilder.append("Unrecognized File/Package(s): ")
                    .append(String.join(", ", foundFiles))
                    .append(".\n");
        }
        if (!filesMissingPackageNames.isEmpty()) {
            stringBuilder.append("File(s) missing a package name: ")
                    .append(String.join(", ", filesMissingPackageNames))
                    .append(".\n");
        }
        if (!incorrectPackageNames.isEmpty()) {
            stringBuilder.append("File(s) with incorrect package name: ")
                    .append(String.join(", ", incorrectPackageNames))
                    .append(".\n");
        }
        if (!stringBuilder.isEmpty()) {
            stringBuilder.append("You may need to reread the phase's specifications for proper project structure.\n")
                    .append("Make sure the files contain the right package name.");
        }
        return stringBuilder.toString();
    }

}
