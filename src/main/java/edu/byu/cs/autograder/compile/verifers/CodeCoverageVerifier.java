package edu.byu.cs.autograder.compile.verifers;

import edu.byu.cs.autograder.GradingContext;
import edu.byu.cs.autograder.GradingException;
import edu.byu.cs.autograder.compile.StudentCodeReader;
import edu.byu.cs.autograder.compile.StudentCodeVerifier;
import edu.byu.cs.model.CoverageRequirement;
import edu.byu.cs.model.Phase;
import edu.byu.cs.util.PhaseUtils;

import java.io.File;
import java.util.Set;
import java.util.TreeSet;

/**
 * Verifies that the necessary packages and classes exists to collect code coverage on unit tests
 */
public class CodeCoverageVerifier implements StudentCodeVerifier {
    private final Set<String> missingPackages = new TreeSet<>();
    private final Set<String> missingFiles = new TreeSet<>();

    @Override
    public void verify(GradingContext context, StudentCodeReader reader) throws GradingException {
        Phase currPhase = context.phase();
        CoverageRequirement coverage = PhaseUtils.unitTestCoverageRequirements(currPhase);
        //FIXME: phases without unit tests
        if (coverage.type() == CoverageRequirement.CoverageType.PACKAGE){
            File packageDirectory = new File(context.stageRepo(), coverage.name());
            if (!packageDirectory.isDirectory()){
                missingPackages.add(coverage.name());
            }
        }
        else{
            if (reader.filesMatching(coverage.name()).anyMatch(x->true)){
                missingFiles.add(coverage.name());
            }
        }

        String message = buildMessage();
        if (!message.isBlank()){
            context.observer().notifyWarning(message);
        }
    }

    private String buildMessage(){
        StringBuilder stringBuilder = new StringBuilder();
        if (!missingPackages.isEmpty()){
            stringBuilder.append("Missing package(s) for Code Coverage: ")
                    .append(String.join(", ", missingPackages))
                    .append(".\n");
        }
        if (!missingFiles.isEmpty()){
            stringBuilder.append("Missing file(s) for Code Coverage: ")
                    .append(String.join(", ", missingFiles))
                    .append(".\n");
        }
        if (!stringBuilder.isEmpty()){
            stringBuilder.append("Code coverage cannot be calculated without the proper project structure.\n")
                    .append("Please double check the phase's specification.");
        }
        return stringBuilder.toString();
    }
}
