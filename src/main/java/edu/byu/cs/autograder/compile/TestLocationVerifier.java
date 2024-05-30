package edu.byu.cs.autograder.compile;

import edu.byu.cs.autograder.GradingContext;
import edu.byu.cs.autograder.GradingException;
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
 * Verifies that the packages used for testing are placed in the right location. Provides a warning
 * to the observer if not and scans the directory for any 'unrecognized' packages and notifies the observer
 * of any found files or directories. If the packages are in the right place, then it verifies that they have
 * the correct
 */
public class TestLocationVerifier implements StudentCodeVerifier {

    private static final Set<String> STUDENT_UNIT_TEST_PACKAGES = Set.of(
            "passoff",
            "service",
            "dataaccess",
            "client"
    );
    private static final String PATH_TO_TEST_JAVA_FROM_MODULE = "src/test/java";
    private static final String DIRECTORY_BEFORE_PACKAGES = "java";
    private static final Integer PACKAGE_DEPTH_SEARCH = 3;
    private final Set<String> missingPackages = new TreeSet<>();
    private final Set<String> foundFiles = new TreeSet<>();
    private final Set<String> incorrectPackageNames = new TreeSet<>();
    private final Set<String> filesMissingPackageNames = new TreeSet<>();
    private final Set<String> visitedModules = new HashSet<>();
    private GradingContext context;

    /**
     * Verifies the student's custom test by checking that they are in the correct directory
     * and the files have the correct package name.
     * @param context A grading context
     * @param reader A student code reader
     * @throws GradingException never
     */
    @Override
    public void verify(GradingContext context, StudentCodeReader reader) throws GradingException {
        this.context = context;

        Phase currPhase = context.phase();
        do {
            for (String unitTestPackagePath : PhaseUtils.unitTestPackagePaths(currPhase)) {
                File packageDirectory = Path.of(context.stageRepo().getPath(), unitTestPackagePath).toFile();
                if (!packageDirectory.isDirectory()) {
                    missingPackages.add(unitTestPackagePath);
                    String module = PhaseUtils.getModuleUnderTest(currPhase);
                    File testJavaDirectory =
                        Path.of(context.stageRepo().getPath(), module, PATH_TO_TEST_JAVA_FROM_MODULE).toFile();
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

        context.observer().notifyWarning(buildMessage());

        missingPackages.clear();
        foundFiles.clear();
        incorrectPackageNames.clear();
        filesMissingPackageNames.clear();
        visitedModules.clear();
    }

    /**
     * Check that the provided test directory contains any unexpected test packages
     * (not one of client, server, dataaccess, passoff) as specified above. If there
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
            for (File childFile : FileUtils.getChildren(unrecognizedPackageFile, PACKAGE_DEPTH_SEARCH)) {
                foundFiles.add(stripOffContextRepo(childFile.toPath()));
            }
        }
    }

    /**
     * Verify that the package's files within it contain the correct
     * package statement at the top of the file.
     * @param packageFile File of a package
     */
    private void verifyPackageDirectory(File packageFile) throws GradingException {
        for (File file : FileUtils.getChildren(packageFile, PACKAGE_DEPTH_SEARCH)) {
            if (file.isDirectory()) continue;
            String packageName = getPackageFromFilePath(file.toPath());
            fileContainsCorrectPackage(file, packageName);
        }
    }

    /**
     *
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
        throw new GradingException("Could not find the package of the following file: " + filePath);
    }

    /**
     * Verify that the file contains the specified packageName
     * @param file A file
     * @param packageName A package name
     */
    private void fileContainsCorrectPackage(File file, String packageName) {
        // Note: could optimize by reading only the first few lines instead of the entire file, but then again
        // who knows about these students ;P
        String fileContents = FileUtils.readStringFromFile(file);
        Pattern pattern = Pattern.compile("^package (.+);");
        Matcher matcher = pattern.matcher(fileContents);
        if (matcher.find()) {
            String filePackageName = matcher.group(1);
            if (!filePackageName.equals(packageName)) {
                incorrectPackageNames.add(file.getName());
            }
        } else {
            filesMissingPackageNames.add(file.getName());
        }
    }

    private String stripOffContextRepo(Path path) {
        Path strippedChildPath = path.subpath(context.stageRepo().toPath().getNameCount(), path.getNameCount());
        return strippedChildPath.toString();
    }

    private String buildMessage() {
        StringBuilder stringBuilder = new StringBuilder();
        if (!missingPackages.isEmpty()) {
            stringBuilder.append("Missing package(s): ")
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
            stringBuilder.append("You may need to reread the phase's specifications for proper project structure.\n" +
                    "Make sure the files contain the right package name");
        }
        return stringBuilder.toString();
    }

}
