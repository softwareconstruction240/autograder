package edu.byu.cs.analytics;

import java.util.Map;

public record CommitsByDay(Map<String, Integer> dayMap, int totalCommits) {
}
