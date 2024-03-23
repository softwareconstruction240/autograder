package edu.byu.cs.autograder.compile;

import edu.byu.cs.autograder.GradingContext;
import edu.byu.cs.autograder.GradingException;
import edu.byu.cs.util.FileUtils;
import edu.byu.cs.util.ProcessUtils;

import java.io.File;

public class CompileHelper {
    private final GradingContext gradingContext;

    public CompileHelper(GradingContext gradingContext) {this.gradingContext = gradingContext;}

    public void compile() throws GradingException {
        verifyProjectStructure();
        modifyPoms();
        packageRepo();
    }

    /**
     * Verifies that the project is structured correctly. The project should be at the top level of the git repository,
     * which is checked by looking for a pom.xml file
     */
    private void verifyProjectStructure() throws GradingException {
        File pomFile = new File(gradingContext.stageRepo(), "pom.xml");
        if (!pomFile.exists()) {
            gradingContext.observer().notifyError("Project is not structured correctly. Your project should be at the top level of your git repository.");
            throw new GradingException("No pom.xml file found");
        }
    }

    private void modifyPoms() {
        File oldRootPom = new File(gradingContext.stageRepo(), "pom.xml");
        File oldServerPom = new File(gradingContext.stageRepo(), "server/pom.xml");
        File oldClientPom = new File(gradingContext.stageRepo(), "client/pom.xml");
        File oldSharedPom = new File(gradingContext.stageRepo(), "shared/pom.xml");

        File newRootPom = new File(gradingContext.phasesPath(), "pom/pom.xml");
        File newServerPom = new File(gradingContext.stageRepo(), "pom/server/pom.xml");
        File newClientPom = new File(gradingContext.stageRepo(), "pom/client/pom.xml");
        File newSharedPom = new File(gradingContext.stageRepo(), "pom/shared/pom.xml");

        FileUtils.copyFile(oldRootPom, newRootPom);
        FileUtils.copyFile(oldServerPom, newServerPom);
        FileUtils.copyFile(oldClientPom, newClientPom);
        FileUtils.copyFile(oldSharedPom, newSharedPom);
    }


    /**
     * Packages the student repo into a jar
     */
    private void packageRepo() throws GradingException {
        gradingContext.observer().update("Packaging repo...");

        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.directory(gradingContext.stageRepo());
        processBuilder.command("mvn", "package", "-DskipTests");
        try {
            ProcessUtils.ProcessOutput output = ProcessUtils.runProcess(processBuilder, 90000); //90 seconds
            if (output.statusCode() != 0) {
                throw new GradingException("Failed to package repo: ", getMavenError(output.stdOut()));
            }
        } catch (ProcessUtils.ProcessException ex) {
            throw new GradingException("Failed to package repo", ex);
        }

        gradingContext.observer().update("Successfully packaged repo");
    }

    /**
     * Retrieves maven error output from maven package stdout
     *
     * @param output A string containing maven standard output
     * @return A string containing maven package error lines
     */
    private String getMavenError(String output) {
        StringBuilder builder = new StringBuilder();
        for (String line : output.split("\n")) {
            if (line.contains("[ERROR] -> [Help 1]")) {
                break;
            }

            if(line.contains("[ERROR]")) {
                String trimLine = line.replace(gradingContext.stageRepo().getAbsolutePath(), "");
                builder.append(trimLine).append("\n");
            }
        }
        return builder.toString();
    }
}
