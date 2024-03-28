package edu.byu.cs.analytics;

import java.util.Map;

/**
 * Contains the results of a commit analytics parse,
 * including the parameters used to generate it.
 *
 * @param dayMap Represents each of the calendar days,
 *               with the number of commits on that day.
 * @param totalCommits The total number of commits processed.
 * @param lowerBoundSeconds Seconds, exclusive.
 * @param upperBoundSeconds Seconds, inclusive.
 */
public record CommitsByDay(
        Map<String, Integer> dayMap,
        int totalCommits,
        long lowerBoundSeconds,
        long upperBoundSeconds
) { }
