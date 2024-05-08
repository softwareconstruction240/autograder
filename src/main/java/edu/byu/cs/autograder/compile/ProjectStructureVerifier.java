package edu.byu.cs.autograder.compile;

import edu.byu.cs.autograder.GradingContext;
import edu.byu.cs.autograder.GradingException;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

/**
 * Verifies that the project is structured correctly. The project should be at the top level of the git repository,
 * which is checked by looking for a pom.xml file
 */
public class ProjectStructureVerifier implements StudentCodeInteractor {
    @Override
    public void interact(GradingContext context, StudentCodeReader reader) throws GradingException {
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
        Set<String> filePaths = new HashSet<>();
        switch (context.phase()) {
            case Phase5, Phase6:
                filePaths.add("client/src/test/java");
            case Phase3, Phase4:
                filePaths.add("server/src/test/java");
                filePaths.add("server/src/main/resources");
            default:
                filePaths.add("shared/src/main/java");
                filePaths.add("shared/src/test/java");
                filePaths.add("server/src/main/java");
                filePaths.add("client/src/main/java");
        }

        for(String filePath : filePaths) {
            File file = new File(context.stageRepo(), filePath);
            if(!file.exists() || !file.isDirectory()) {
                context.observer().notifyWarning("Directory %s could not be found".formatted(filePath));
            }
        }
    }
}
