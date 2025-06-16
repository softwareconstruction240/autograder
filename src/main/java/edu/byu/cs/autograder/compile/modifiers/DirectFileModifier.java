package edu.byu.cs.autograder.compile.modifiers;

import edu.byu.cs.autograder.GradingContext;
import edu.byu.cs.autograder.compile.StudentCodeModifier;
import edu.byu.cs.util.FileUtils;

import java.io.File;

/**
 * Provides a helper method used by other implementations of {@link StudentCodeModifier}
 * that performs the getting of the old file and new file and does the replacement directly.
 */
public abstract class DirectFileModifier implements StudentCodeModifier {
    protected void replaceFile(GradingContext context, String oldPath, String newPath) {
        File oldFile = new File(context.stageRepo(), oldPath);
        File newFile = new File(context.phasesPath(), newPath);
        FileUtils.copyFile(oldFile, newFile);
    }
}
