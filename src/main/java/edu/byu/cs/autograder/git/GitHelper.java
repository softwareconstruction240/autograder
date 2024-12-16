package edu.byu.cs.autograder.git;

import edu.byu.cs.autograder.GradingException;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class GitHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(GitHelper.class);

    public static void fetchRepo(File intoDirectory, String repoUrl) throws GradingException {
        CloneCommand cloneCommand = Git.cloneRepository()
                .setURI(repoUrl)
                .setDirectory(intoDirectory);
        try (Git git = cloneCommand.call()) {
            LOGGER.info("Cloned repo to {}", git.getRepository().getDirectory());
        } catch (GitAPIException e) {
            throw new GradingException("Failed to clone repo: " + e.getMessage(), e);
        }
    }
}
