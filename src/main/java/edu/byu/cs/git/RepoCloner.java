package edu.byu.cs.git;

import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.File;

public class RepoCloner {

    /**
     * Clones the repo at the given url to the given local path. The name
     * @param repoUrl the url of the repo to clone, e.g. <a href="https://github.com/softwareconstruction240/chess.git">https://github.com/softwareconstruction240/chess.git</a>
     * @param localPath the path to clone the repo to, e.g. /submissions/bonjovi/phase1
     */
    public static void fetchRepo(String repoUrl, String localPath) {
        CloneCommand cloneCommand = Git.cloneRepository()
                .setURI(repoUrl)
                .setDirectory(new File(localPath));

        try (Git git = cloneCommand.call()) {
            System.out.println("Cloned repo to " + git.getRepository().getDirectory());
        } catch (GitAPIException e) {
            throw new RuntimeException(e);
        }

    }

    public static void main(String[] args) {
        fetchRepo("https://github.com/softwareconstruction240/chess.git", "./chess-submission");
    }
}
