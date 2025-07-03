package edu.byu.cs.autograder.compile.verifers;

import edu.byu.cs.autograder.GradingContext;
import edu.byu.cs.autograder.GradingException;
import edu.byu.cs.autograder.compile.StudentCodeReader;
import edu.byu.cs.model.Phase;
import edu.byu.cs.util.FileUtils;
import edu.byu.cs.util.PhaseUtils;
import edu.byu.cs.util.ProcessUtils;

import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

/**
 * Verifies that the test files have not been modified or are missing. Provides a warning to the observer
 * of any files found to be modified or missing.
 */
public class ModifiedTestFilesVerifier extends ModifiedFilesVerifier {

    public ModifiedTestFilesVerifier() {
        super(Set.of(
                "CastlingTests.java",
                "EnPassantTests.java",
                "TestFactory.java",
                "deleteme"
        ), new String[]{
                ".*shared/src/test/java/passoff/.*\\.java", // shared passoff
                ".*server/src/test/java/passoff/.*\\.java"  // server passoff
        }, "test");
    }

    /**
     * Checks if the student modified or is missing test files by comparing the relevant phase test
     * files to the student's files.
     * Algorithm: Working from the current phase back, for each reference file in the phase, compare
     * the reference file to the student's equivalent.
     *
     * @param context The grading context for the student's submission
     * @param reader The reader for the student's files in their submission
     * @throws GradingException if there is some error when comparing the student's test files
     * to the reference test files (specifically ProcessException)
     */
    @Override
    protected void checkForModifiedOrMissingFiles(GradingContext context, StudentCodeReader reader) throws GradingException {
        Map<String, String> studentTestFileNames = getStudentFileNamesToAbsolutePath(reader);
        Phase currentPhase = context.phase();
        do {
            Map<String, String> referencePhaseFileNames =
                    getPhasePassoffFileNamesToAbsolutePath(context.phasesPath(), currentPhase);
            try {
                compareReferenceFilesToStudent(referencePhaseFileNames, studentTestFileNames);
            } catch (ProcessUtils.ProcessException e) {
                throw new GradingException("Unable to verify unmodified test files: " + e.getMessage());
            }
            currentPhase = PhaseUtils.getPreviousPhase(currentPhase);
        } while (currentPhase != null);
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
}