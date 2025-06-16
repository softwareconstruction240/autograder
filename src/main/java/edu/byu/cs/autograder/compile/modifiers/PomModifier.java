package edu.byu.cs.autograder.compile.modifiers;

import edu.byu.cs.autograder.GradingContext;

/**
 * Replaces the pom files in the staged repo with the pom files stored locally.
 * This ensures that the pom files haven't been modified or deleted problematically
 * and that no external dependencies have been installed.
 */
public class PomModifier extends DirectFileModifier {
    @Override
    public void modify(GradingContext context) {
        replaceFile(context, "pom.xml", "pom/pom.xml");
        replaceFile(context, "server/pom.xml", "pom/server/pom.xml");
        replaceFile(context, "client/pom.xml", "pom/client/pom.xml");
        replaceFile(context, "shared/pom.xml", "pom/shared/pom.xml");
        replaceFile(context, "test-dependencies-assembly.xml", "pom/test-dependencies-assembly.xml");
    }
}
