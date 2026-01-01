package edu.byu.cs.autograder.compile.verifers;

import edu.byu.cs.autograder.GradingException;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Verifies that the packages used for testing are placed in the correct location. Provides a warning
 * to the observer if not and scans the directory for any 'unrecognized' packages and notifies the observer
 * of any found files or directories.
 */
public class TestLocationVerifier extends TestFileVerifier {

    private static final String DIRECTORY_BEFORE_PACKAGES = "java";
    private final Set<String> missingPackages = new TreeSet<>();
    private final Set<String> incorrectPackageNames = new TreeSet<>();
    private final Set<String> filesMissingPackageNames = new TreeSet<>();

    @Override
    protected void clearSets() {
        missingPackages.clear();
        incorrectPackageNames.clear();
        filesMissingPackageNames.clear();
    }

    /**
     * Verify that the file contains the correct
     * package statement at the top of the file.
     *
     * @param file A file
     */
    @Override
    protected void verifyPackageFile(File file) throws GradingException {
        String packageName = getPackageFromFilePath(file.toPath());

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

    @Override
    protected String buildMessage() {
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
