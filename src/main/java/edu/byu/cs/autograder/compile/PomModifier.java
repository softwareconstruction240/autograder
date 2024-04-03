package edu.byu.cs.autograder.compile;

import edu.byu.cs.autograder.GradingContext;
import edu.byu.cs.util.FileUtils;

import java.io.File;

public class PomModifier implements StudentCodeModifier {
    @Override
    public void modifyCode(GradingContext context) {
        File oldRootPom = new File(context.stageRepo(), "pom.xml");
        File oldServerPom = new File(context.stageRepo(), "server/pom.xml");
        File oldClientPom = new File(context.stageRepo(), "client/pom.xml");
        File oldSharedPom = new File(context.stageRepo(), "shared/pom.xml");
        File oldAssembly = new File(context.stageRepo(), "test-dependencies-assembly.xml");

        File newRootPom = new File(context.phasesPath(), "pom/pom.xml");
        File newServerPom = new File(context.phasesPath(), "pom/server/pom.xml");
        File newClientPom = new File(context.phasesPath(), "pom/client/pom.xml");
        File newSharedPom = new File(context.phasesPath(), "pom/shared/pom.xml");
        File newAssembly = new File(context.phasesPath(), "pom/test-dependencies-assembly.xml");

        FileUtils.copyFile(oldRootPom, newRootPom);
        FileUtils.copyFile(oldServerPom, newServerPom);
        FileUtils.copyFile(oldClientPom, newClientPom);
        FileUtils.copyFile(oldSharedPom, newSharedPom);
        FileUtils.copyFile(oldAssembly, newAssembly);
    }
}
