package edu.byu.cs.autograder.git;

import org.eclipse.jgit.api.Git;

import java.io.Closeable;
import java.io.File;

record RepoContext(
        Git git,
        String testName,
        File directory,
        File changeFile,
        String changeFilename
) implements Closeable {
    @Override
    public void close() {
        if (this.git != null) this.git.close();
    }
}
