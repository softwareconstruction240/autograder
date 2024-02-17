package edu.byu.cs.autograder;

/**
 * Holds settings related to grading the phases.
 *
 * @param MAX_PENALIZE_DAYS_LATE Lose credit for days late UP TO this value
 * @param LATE_PENALTY_PCT_PER_DAY x% per day will be subtracted
 */
public record GradingSettings(
        int MAX_PENALIZE_DAYS_LATE,
        int LATE_PENALTY_PCT_PER_DAY
) { }
