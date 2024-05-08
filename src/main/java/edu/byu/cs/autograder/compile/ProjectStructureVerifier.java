package edu.byu.cs.autograder.compile;

import edu.byu.cs.autograder.GradingContext;
import edu.byu.cs.autograder.GradingException;
import edu.byu.cs.model.Phase;

import java.io.File;

/**
 * Verifies that the project is structured correctly. The project should be at the top level of the git repository,
 * which is checked by looking for a pom.xml file
 */
public class ProjectStructureVerifier implements StudentCodeModifier {
    @Override
    public void modifyCode(GradingContext context) throws GradingException {
        verifyRootPom(context);
        verifyDirectoryStructure(context);
    }

    private void verifyRootPom(GradingContext context) throws GradingException {
        File pomFile = new File(context.stageRepo(), "pom.xml");
        if (!pomFile.exists()) {
            throw new GradingException("Project is not structured correctly. Your project should be at the top level of your git repository.");
        }
    }

    private void verifyDirectoryStructure(GradingContext context) {
        verifyDirectory(context, "shared/src/main/java");
        verifyDirectory(context, "shared/src/test/java");
        verifyDirectory(context, "server/src/main/java");
        verifyDirectory(context, "client/src/main/java");
        if(context.phase() == Phase.Phase0 || context.phase() == Phase.Phase1) return;

        verifyDirectory(context, "server/src/test/java");
        verifyDirectory(context,  "server/src/main/resources");
        if(context.phase() == Phase.Phase3 || context.phase() == Phase.Phase4) return;

        verifyDirectory(context,  "client/src/test/java");
    }

    private void verifyDirectory(GradingContext context, String filePath) {
        File file = new File(context.stageRepo(), filePath);
        if(!file.exists() || !file.isDirectory()) {
            context.observer().notifyWarning("Directory %s could not be found".formatted(filePath));
        }
    }
}
