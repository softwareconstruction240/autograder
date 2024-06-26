package edu.byu.cs.autograder.compile.modifiers;

import edu.byu.cs.autograder.GradingContext;
import edu.byu.cs.autograder.compile.StudentCodeModifier;
import edu.byu.cs.util.FileUtils;

import java.io.File;

public class PassoffJarModifier implements StudentCodeModifier {
    @Override
    public void modify(GradingContext context) {
        File oldJar = new File(context.stageRepo(), "server/lib/passoff-dependencies.jar");
        File newJar = new File(context.phasesPath(), "libs/passoff-dependencies.jar");
        FileUtils.copyFile(oldJar, newJar);
    }
}
