package edu.byu.cs.autograder.compile.modifiers;

import edu.byu.cs.autograder.GradingContext;

/**
 * Replaces the passoff-dependencies jar in the staged repo with the passoff-dependencies jar
 * stored locally. This ensures the tests use the most current version of the jar.
 */
public class PassoffJarModifier extends DirectFileModifier {
    @Override
    public void modify(GradingContext context) {
        replaceFile(context, "server/lib/passoff-dependencies.jar", "libs/passoff-dependencies.jar");
    }
}
