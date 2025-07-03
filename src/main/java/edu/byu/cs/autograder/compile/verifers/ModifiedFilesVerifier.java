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

public abstract class ModifiedFilesVerifier implements StudentCodeVerifier {

    private final Set<String> ignoredFiles;
    private final Set<String> studentFilesRegex;

    private final String fileType;

    private final Set<String> modifiedFiles = new HashSet<>();
    private final Set<String> missingFiles = new HashSet<>();

    public ModifiedFilesVerifier(Set<String> setIgnoredFiles, Set<String> setStudentFilesRegex, String setFileType) {
        ignoredFiles = setIgnoredFiles;
        studentFilesRegex = setStudentFilesRegex;
        fileType = setFileType;
    }

    @Override
    public void verify(GradingContext context, StudentCodeReader reader) throws GradingException {
        if (!PhaseUtils.isPhaseGraded(context.phase())) return;

        // check for modified or missing files
        checkForModifiedOrMissingFiles();

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

    protected abstract void checkForModifiedOrMissingFiles();

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
    private Map<String, String> getStudentFileNamesToAbsolutePath(StudentCodeReader reader) {
        Map<String, String> studentPassoffFileNamesToAbsolutes = new HashMap<>();
        for (String passoffRegex : studentFilesRegex) {
            studentPassoffFileNamesToAbsolutes.putAll(reader.getFileNameToAbsolutePath(passoffRegex));
        }
        return studentPassoffFileNamesToAbsolutes;
    }

    /**
     * Gets all the phases' file names based on the phase number and path to the phases folder
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
     * Compares the reference files to the student's files.
     * If there is a modified or missing file, it is added to the `modifiedFiles` and `missingFiles` fields as
     * a side effect. Utilizes a process with the `diff` command.
     * @param referenceFileNames A map of the reference's file names to their absolute paths
     * @param studentFileNames A map of the student's file names to their absolute paths
     * @throws ProcessUtils.ProcessException if process times outs out
     */
    private void compareReferenceFilesToStudent(
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