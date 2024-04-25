package edu.byu.cs.analytics;

import java.util.Map;

/**
 * Contains the results of a commit analytics parse,
 * including the parameters used to generate it.
 *
 * @param dayMap Represents each of the calendar days,
 *               with the number of commits on that day.
 * @param totalCommits The total number of commits processed, excluding merge commits.
 * @param mergeCommits The total number of merge commits.
 * @param commitsInOrder Reports whether all commits were authored strictly after their parents.
 * @param commitsInFuture Reports whether any commits were found after the end point chronologically.
 * @param lowerThreshold The {@link CommitThreshold}, exclusive.
 * @param upperThreshold The {@link CommitThreshold}, inclusive.
 */
public record CommitsByDay(
        Map<String, Integer> dayMap,
        int totalCommits,
        int mergeCommits,
        boolean commitsInOrder,
        boolean commitsInFuture,
        CommitThreshold lowerThreshold,
        CommitThreshold upperThreshold
) { }
