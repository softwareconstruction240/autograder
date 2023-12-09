package edu.byu.cs.autograder;

import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.File;

/**
 * A template for fetching, compiling, and running student code
 */
public abstract class Grader {

    private final String repoUrl;
    private final String localPath;

    public Grader(String repoUrl, String localPath) {
        this.repoUrl = repoUrl;
        this.localPath = localPath;
    }

    public void run() {
        fetchRepo(repoUrl, localPath);
        compile();
        runTests();
    }

    private void fetchRepo(String repoUrl, String localPath) {
        CloneCommand cloneCommand = Git.cloneRepository()
                .setURI(repoUrl)
                .setDirectory(new File(localPath));

        try (Git git = cloneCommand.call()) {
            System.out.println("Cloned repo to " + git.getRepository().getDirectory());
        } catch (GitAPIException e) {
            throw new RuntimeException(e);
        }
    }

    protected abstract void compile();

    protected abstract void runTests();

}
