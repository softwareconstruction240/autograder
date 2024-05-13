package edu.byu.cs.autograder.git;

import org.eclipse.jgit.api.Git;

import java.io.File;

record RepoContext(
        Git git,
        String testName,
        File directory,
        File changeFile,
        String changeFilename
) {
}
