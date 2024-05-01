package edu.byu.cs.autograder.git.RepoGenerationCommands;

import edu.byu.cs.autograder.git.GitRepoState;

import java.util.Arrays;
import java.util.LinkedList;

/**
 * Generates a sequence of commits, each with incrementing values.
 * They will all by applied so that they are N days separated from each other.
 *
 * @param daysBetweenCommits
 */
public record CommitTiming(
        int... daysBetweenCommits
) implements GitGenerationCommand {
    @Override
    public int getDaysChanged() {
        return Arrays.stream(daysBetweenCommits).sum();
    }

    @Override
    public ProcessBuilder run(int absoluteDaysAgo, GitRepoState repoState) {
        var commandList = new LinkedList<String>();
        for (var daysSinceCommit : daysBetweenCommits) {
            commandList.add(repoState.scriptRoot + "commit.sh \"Change %d\" %d && ".formatted(
                    repoState.makeChange(), absoluteDaysAgo + daysSinceCommit));
        }

        commandList.addLast("echo \"Submitted %d commits\"".formatted(commandList.size()));
        return new ProcessBuilder(commandList);
    }
}
