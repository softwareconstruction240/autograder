package edu.byu.cs.autograder.compile.verifers;

import java.io.File;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Verifies that the unit test files are named using the expected pattern. Provides a warning
 * to the observer if not and scans the directory for any 'unrecognized' files and notifies the observer
 * of any found files.
 */
public class TestClassNameVerifier extends TestFileVerifier {

    private final Set<String> incorrectFileNames = new TreeSet<>();

    @Override
    protected void clearSets() {
        incorrectFileNames.clear();
    }

    /**
     * Verify that the file's name matches the required pattern.
     *
     * @param file A file
     */
    @Override
    protected void verifyPackageFile(File file) {
        Pattern pattern = Pattern.compile("^.+Tests?\\.java$");
        Matcher matcher = pattern.matcher(file.getName());
        if (!matcher.find()) {
            incorrectFileNames.add(file.getName());
        }
    }

    @Override
    protected String buildMessage() {
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
