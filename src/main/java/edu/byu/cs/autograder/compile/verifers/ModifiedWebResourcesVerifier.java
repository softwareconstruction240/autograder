package edu.byu.cs.autograder.compile.verifers;

import edu.byu.cs.autograder.GradingContext;
import edu.byu.cs.autograder.GradingException;
import edu.byu.cs.autograder.compile.StudentCodeReader;
import edu.byu.cs.model.Phase;
import edu.byu.cs.util.FileUtils;
import edu.byu.cs.util.PhaseUtils;

import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

public class ModifiedWebResourcesVerifier extends ModifiedFilesVerifier {

    public ModifiedWebResourcesVerifier() {
        super(Set.of(
                "deleteme"
        ), new String[] {
                ".*server/src/main/resources/web/.*"
        }, "web resources");
    }

    /**
     * Checks if the student modified or is missing web resource files by comparing the relevant phase
     * web resource files to the student's files.
     *
     * @param context The grading context for the student's submission
     * @param reader The reader for the student's files in their submission
     * @throws GradingException if there is some error when comparing the student's files
     */
    @Override
    protected void checkForModifiedOrMissingFiles(GradingContext context, StudentCodeReader reader)
            throws GradingException {
        // check if current phase is phase 3 or beyond
        Phase phase = context.phase();
        int phaseNumber = Integer.parseInt(PhaseUtils.getPhaseAsString(phase));
        if (phaseNumber<3) return;

        // get student and reference files and compare
        Map<String, String> studentWebResourcesNames = getStudentFileNamesToAbsolutePath(reader);
        String webResourcesPath = String.format("%s/phase3/resources/web/", context.phasesPath());
        Map<String, String> referenceWebResourcesNames =
                FileUtils.getFileNamesToAbsolutePaths(Path.of(webResourcesPath));
        compareFilesToStudent(referenceWebResourcesNames, studentWebResourcesNames);
    }
}
