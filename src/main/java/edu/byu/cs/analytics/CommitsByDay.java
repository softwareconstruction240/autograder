package edu.byu.cs.analytics;

import java.util.List;
import java.util.Map;

/**
 * Contains the results of a commit analytics parse,
 * including the parameters used to generate it.
 * <br>
 * This data structure is intended for use <i>only</i> within the system processing memory.
 * It is not intended to be persisted in a database or other data structure.
 *
 * @param dayMap Represents each of the calendar days,
 *               with the number of commits on that day.
 * @param changesPerCommit One entry for each commit processed, representing the number of lines changed in the commit.
 * @param totalCommits The total number of commits processed, excluding merge commits.
 * @param mergeCommits The total number of merge commits.
 * @param commitsInOrder Reports whether all commits were authored strictly after their parents.
 * @param commitsInFuture Reports whether any commits were found after the end point chronologically.
 * @param lowerThreshold The {@link CommitThreshold}, exclusive.
 * @param upperThreshold The {@link CommitThreshold}, inclusive.
 */
public record CommitsByDay(
        Map<String, Integer> dayMap,
        List<Integer> changesPerCommit,
        int totalCommits,
        int mergeCommits,
        boolean commitsInOrder,
        boolean commitsInFuture,
        CommitThreshold lowerThreshold,
        CommitThreshold upperThreshold
) { }
