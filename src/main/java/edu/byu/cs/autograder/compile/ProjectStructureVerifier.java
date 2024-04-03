package edu.byu.cs.autograder.compile;

import edu.byu.cs.autograder.GradingContext;
import edu.byu.cs.autograder.GradingException;

import java.io.File;

/**
 * Verifies that the project is structured correctly. The project should be at the top level of the git repository,
 * which is checked by looking for a pom.xml file
 */
public class ProjectStructureVerifier implements StudentCodeModifier {
    @Override
    public void modifyCode(GradingContext context) throws GradingException {
        File pomFile = new File(context.stageRepo(), "pom.xml");
        if (!pomFile.exists()) {
            throw new GradingException("Project is not structured correctly. Your project should be at the top level of your git repository.");
        }
    }
}
