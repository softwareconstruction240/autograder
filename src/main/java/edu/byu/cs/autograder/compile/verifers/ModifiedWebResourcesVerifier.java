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
        super(Set.of("deleteme"),
                new String[] {
                        ".*server/src/main/resources/web/.*"
                }, "web resources");
    }

    @Override
    protected void checkForModifiedOrMissingFiles(GradingContext context, StudentCodeReader reader) throws GradingException {
        //Check if phase is phase 3 or beyond
        Phase phase = context.phase();
        int phaseNumber = Integer.parseInt(PhaseUtils.getPhaseAsString(phase));
        if(phaseNumber<3) return;

        Map<String, String> studentWebResourcesFileNames = getStudentFileNamesToAbsolutePath(reader);
        String webResourcesPath = String.format("%s/phase3/resources/web/", context.phasesPath());
        Map<String, String> referenceWebResourcesFileNames = FileUtils.getFileNamesToAbsolutePaths(Path.of(webResourcesPath));
        compareFilesToStudent(referenceWebResourcesFileNames, studentWebResourcesFileNames);
    }
}
