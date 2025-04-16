package edu.byu.cs.autograder.compile.modifiers;

import edu.byu.cs.autograder.GradingContext;
import edu.byu.cs.autograder.compile.StudentCodeModifier;
import edu.byu.cs.util.FileUtils;

import java.io.File;

/**
 * Replaces the passoff-dependencies jar in the staged repo with the passoff-dependencies jar
 * stored locally. This ensures the tests use the most current version of the jar.
 */
public class PassoffJarModifier implements StudentCodeModifier {
    @Override
    public void modify(GradingContext context) {
        File oldJar = new File(context.stageRepo(), "server/lib/passoff-dependencies.jar");
        File newJar = new File(context.phasesPath(), "libs/passoff-dependencies.jar");
        FileUtils.copyFile(oldJar, newJar);
    }
}
