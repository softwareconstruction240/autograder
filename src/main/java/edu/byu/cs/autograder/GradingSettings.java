package edu.byu.cs.autograder;

/**
 * Holds settings related to grading the phases.
 *
 * @param MAX_PENALIZE_DAYS_LATE Lose credit for days late UP TO this value
 * @param LATE_PENALTY_PCT_PER_DAY x% per day will be subtracted
 * @param REQUIRED_COMMITS The required number of commits (since the last phase) to be able to pass off
 */
public record GradingSettings(
        int MAX_PENALIZE_DAYS_LATE,
        int LATE_PENALTY_PCT_PER_DAY,
        int REQUIRED_COMMITS
) { }
