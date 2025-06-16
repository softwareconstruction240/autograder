package edu.byu.cs.autograder.compile.verifers;

import edu.byu.cs.autograder.GradingContext;
import edu.byu.cs.autograder.GradingException;
import edu.byu.cs.autograder.compile.StudentCodeReader;
import edu.byu.cs.autograder.compile.StudentCodeVerifier;
import edu.byu.cs.model.Phase;
import edu.byu.cs.util.FileUtils;
import edu.byu.cs.util.PhaseUtils;
import edu.byu.cs.util.ProcessUtils;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Verifies that the test files have not been modified or are missing. Provides a warning to the observer
 * of any files found to be modified or missing.
 */
public class ModifiedTestFilesVerifier implements StudentCodeVerifier {

    private static final Set<String> IGNORED_FILES = Set.of(
            "CastlingTests.java",
            "EnPassantTests.java",
            "TestFactory.java",
            "deleteme"
    );

    private static final String[] STUDENT_PASSOFF_FILES_REGEX = {
            ".*shared/src/test/java/passoff/.*\\.java", // shared passoff
            ".*server/src/test/java/passoff/.*\\.java"  // server passoff
    };

    private final Set<String> modifiedFiles = new HashSet<>();
    private final Set<String> missingFiles = new HashSet<>();

    /**
     * Checks if the student modified or is missing test files by comparing the relevant phase test
     * files to the student's files.
     * Algorithm: Working from the current phase back, for each reference file in the phase, compare
     * the reference file to the student's equivalent. If there is a modified file or no equivalent file,
     * the observer is notified. Utilizes a `process` with the `diff` command.
     *
     * @param context The grading context for the student's submission
     * @param reader The reader for the student's files in their submission
     * @throws GradingException if there is some error when comparing the student's test files
     * to the reference test files (specifically ProcessException)
     */
    @Override
    public void verify(GradingContext context, StudentCodeReader reader) throws GradingException {
        if (!PhaseUtils.isPhaseGraded(context.phase())) return;

        // check for modified or missing files
        Map<String, String> studentTestFileNames = getStudentPassoffFileNamesToAbsolutePath(reader);
        Phase currentPhase = context.phase();
        do {
            Map<String, String> referencePhaseFileNames =
                    getPhasePassoffFileNamesToAbsolutePath(context.phasesPath(), currentPhase);
            try {
                comparePhaseReferencePassoffFilesToStudent(referencePhaseFileNames, studentTestFileNames);
            } catch (ProcessUtils.ProcessException e) {
                throw new GradingException("Unable to verify unmodified test files: " + e.getMessage());
            }
            currentPhase = PhaseUtils.getPreviousPhase(currentPhase);
        } while (currentPhase != null);

        // notify observer if there are modified or missing files
        if (!modifiedFiles.isEmpty() || !missingFiles.isEmpty()) {
            String warningMessage = String.format(
                    """
                    Warning: your test files have changed. This could lead to the autograder giving
                    different results than your local machine.
                    %s
                    %s
                    """,
                    !modifiedFiles.isEmpty() ? "Modified Files: " + String.join(", ", modifiedFiles) : "",
                    !missingFiles.isEmpty() ? "Missing Files: " + String.join(", ", missingFiles) : ""
            );
            context.observer().notifyWarning(warningMessage);
            modifiedFiles.clear();
            missingFiles.clear();
        }
    }

    /**
     * In the student's repository, gets all the file names to their absolute path based on
     * the STUDENT_PASSOFF_FILES_REGEX
     * For example:
     * {
     *      "ChessBoardTests.java":
     *      "IdeaProjects/autograder/tmp-###-###/repo/shared/src/test/java/passoff/chess/ChessBoardTests.java"
     * }
     * @param reader Student code reader
     * @return A map of the file's name and the associated absolute path to that file
     */
    private Map<String, String> getStudentPassoffFileNamesToAbsolutePath(StudentCodeReader reader) {
        Map<String, String> studentPassoffFileNamesToAbsolutes = new HashMap<>();
        for (String passoffRegex : STUDENT_PASSOFF_FILES_REGEX) {
            studentPassoffFileNamesToAbsolutes.putAll(reader.getFileNameToAbsolutePath(passoffRegex));
        }
        return studentPassoffFileNamesToAbsolutes;
    }

    /**
     * Gets all the phases' test file names based on the phase number and path to the phases folder
     * containing those files.
     * Format:
     * {
     *      "ChessBoardTests.java":
     *      "IdeaProjects/autograder/phases/phase0/passoff/chess/ChessBoardTests.java"
     * }
     * @param phase Phase to grab the test files from.
     * @return A map of the phase's file names and the associated absolute path to that file.
     */
    private Map<String, String> getPhasePassoffFileNamesToAbsolutePath(
            String phasesPath,
            Phase phase
    ) throws GradingException {
        String phaseNumber = PhaseUtils.getPhaseAsString(phase);
        String passoffPath = String.format("%s/phase%s/passoff/", phasesPath, phaseNumber);
        return FileUtils.getFileNamesToAbsolutePaths(Path.of(passoffPath));
    }

    /**
     * Compares the reference phase passoff test files to the student's test files.
     * If there is a modified or missing file, it is added to the `modifiedFiles` and `missingFiles` fields as
     * a side effect. Utilizes a process with the `diff` command.
     * @param referencePhaseFileNames A map of the phase's file names to their absolute paths
     * @param studentFileNames A map of the student's passoff file names to their absolute paths
     * @throws ProcessUtils.ProcessException if process times outs out
     */
    private void comparePhaseReferencePassoffFilesToStudent(
            Map<String, String> referencePhaseFileNames,
            Map<String, String> studentFileNames
    ) throws ProcessUtils.ProcessException {
        ProcessBuilder processBuilder = new ProcessBuilder();
        for (String referenceFileName : referencePhaseFileNames.keySet()) {
            if (IGNORED_FILES.contains(referenceFileName)) {
                continue;
            }

            String referenceAbsolutePath = referencePhaseFileNames.get(referenceFileName);
            String studentAbsolutePath = studentFileNames.get(referenceFileName);
            if (studentAbsolutePath == null) {
                missingFiles.add(referenceFileName);
                continue;
            }

            // add -u option only if we want to output the differences nicely to the observer in the future.
            processBuilder.command(
                    "diff",
                    "--ignore-all-space",
                    "--ignore-blank-lines",
                    referenceAbsolutePath,
                    studentAbsolutePath
            );

            ProcessUtils.ProcessOutput processOutput = ProcessUtils.runProcess(processBuilder);
            if (!processOutput.stdOut().isEmpty()) {
                modifiedFiles.add(referenceFileName);
            }
        }
    }

}
