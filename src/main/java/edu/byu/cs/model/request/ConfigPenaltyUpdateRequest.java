package edu.byu.cs.model.request;

/**
 * Represents a request for updating the values used for calculating penalties
 *
 * @param perDayLatePenalty the percentage taken off submission for everyday late
 * @param maxLateDaysPenalized the maximum number of days the late penalty should apply
 * @param gitCommitPenalty the penalty to apply for missing commits
 * @param linesChangedPerCommit the minimum number of lines needed for a commit to count
 * @param clockForgivenessMinutes the number of minutes a commit can be authored
 *                                past the time of submission
 */
public record ConfigPenaltyUpdateRequest(
        float perDayLatePenalty,
        int maxLateDaysPenalized,
        float gitCommitPenalty,
        int linesChangedPerCommit,
        int clockForgivenessMinutes
) {}
