package edu.byu.cs.autograder.compile.verifers;

import edu.byu.cs.autograder.GradingContext;
import edu.byu.cs.autograder.GradingException;
import edu.byu.cs.autograder.compile.StudentCodeReader;
import edu.byu.cs.autograder.compile.StudentCodeVerifier;
import edu.byu.cs.util.PhaseUtils;
import edu.byu.cs.util.ProcessUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public abstract class ModifiedFilesVerifier implements StudentCodeVerifier {

    private final Set<String> ignoredFiles;
    private final String[] studentFilesRegex;

    private final String fileType;

    private final Set<String> modifiedFiles = new HashSet<>();
    private final Set<String> missingFiles = new HashSet<>();

    public ModifiedFilesVerifier(Set<String> setIgnoredFiles, String[] setStudentFilesRegex, String setFileType) {
        ignoredFiles = setIgnoredFiles;
        studentFilesRegex = setStudentFilesRegex;
        fileType = setFileType;
    }

    /**
     * Checks if the student modified or is missing files by comparing the relevant phase
     * files to the student's files.
     * The algorithm that does this is determined by a concrete class the extends the
     * {@code ModifiedFilesVerifier}. If there is a modified file or no equivalent file,
     * the observer is notified. Utilizes a `process` with the `diff` command.
     *
     * @param context The grading context for the student's submission
     * @param reader The reader for the student's files in their submission
     * @throws GradingException if there is some error when comparing files
     */
    @Override
    public void verify(GradingContext context, StudentCodeReader reader) throws GradingException {
        if (!PhaseUtils.isPhaseGraded(context.phase())) return;

        // check for modified or missing files
        checkForModifiedOrMissingFiles(context, reader);

        // notify observer if there are modified or missing files
        if (!modifiedFiles.isEmpty() || !missingFiles.isEmpty()) {
            String warningMessage = String.format(
                    """
                    Warning: your %s files have changed. This could lead to the autograder giving
                    different results than your local machine.
                    %s
                    %s
                    """,
                    fileType,
                    !modifiedFiles.isEmpty() ? "Modified Files: " + String.join(", ", modifiedFiles) : "",
                    !missingFiles.isEmpty() ? "Missing Files: " + String.join(", ", missingFiles) : ""
            );
            context.observer().notifyWarning(warningMessage);
            modifiedFiles.clear();
            missingFiles.clear();
        }
    }

    /**
     * Checks if the student modified or is missing files by comparing the relevant phase
     * files to the student's files. For each reference file in the phase, compare the
     * reference file to the student's equivalent.
     *
     * @param context The grading context for the student's submission
     * @param reader The reader for the student's files in their submission
     * @throws GradingException if there is some error when comparing the student's files
     */
    protected abstract void checkForModifiedOrMissingFiles(GradingContext context, StudentCodeReader reader)
            throws GradingException;

    /**
     * Compares the reference files to the student's files.
     *
     * @param referenceFileNames A map of the reference's file names to their absolute paths
     * @param studentFileNames A map of the student's file names to their absolute paths
     * @throws GradingException if an issue arises running the process to verify the files
     */
    protected void compareFilesToStudent(
            Map<String, String> referenceFileNames,
            Map<String, String> studentFileNames
    ) throws GradingException {
        try {
            compareReferenceFilesToStudent(referenceFileNames, studentFileNames);
        } catch (ProcessUtils.ProcessException e) {
            throw new GradingException("Unable to verify unmodified " + fileType + " files: " + e.getMessage());
        }
    }

    /**
     * In the student's repository, gets all the file names to their absolute path based on
     * the STUDENT_FILES_REGEX
     * For example:
     * {
     *      "ChessBoardTests.java":
     *      "IdeaProjects/autograder/tmp-###-###/repo/shared/src/test/java/passoff/chess/ChessBoardTests.java"
     * }
     * @param reader Student code reader
     * @return A map of the file's name and the associated absolute path to that file
     */
    protected Map<String, String> getStudentFileNamesToAbsolutePath(StudentCodeReader reader) {
        Map<String, String> studentPassoffFileNamesToAbsolutes = new HashMap<>();
        for (String passoffRegex : studentFilesRegex) {
            studentPassoffFileNamesToAbsolutes.putAll(reader.getFileNameToAbsolutePath(passoffRegex));
        }
        return studentPassoffFileNamesToAbsolutes;
    }

    /**
     * Compares the reference files to the student's files.
     * If there is a modified or missing file, it is added to the `modifiedFiles` and `missingFiles` fields as
     * a side effect. Utilizes a process with the `diff` command.
     * @param referenceFileNames A map of the reference's file names to their absolute paths
     * @param studentFileNames A map of the student's file names to their absolute paths
     * @throws ProcessUtils.ProcessException if process times out
     */
    protected void compareReferenceFilesToStudent(
            Map<String, String> referenceFileNames,
            Map<String, String> studentFileNames
    ) throws ProcessUtils.ProcessException {
        ProcessBuilder processBuilder = new ProcessBuilder();
        for (String referenceFileName : referenceFileNames.keySet()) {
            if (ignoredFiles.contains(referenceFileName)) {
                continue;
            }

            String referenceAbsolutePath = referenceFileNames.get(referenceFileName);
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