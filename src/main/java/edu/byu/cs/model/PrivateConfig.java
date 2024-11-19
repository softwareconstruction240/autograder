package edu.byu.cs.model;

public record PrivateConfig(
    PenaltyConfig penalty,
    IdConfig ids
) {
    public record PenaltyConfig(
        float perDayLatePenalty,
        float gitCommitPenalty,
        int maxLateDaysPenalized,
        int linesChangedPerCommit,
        int clockForgivenessMinutes
    ){}

    public record IdConfig(

    ){}
}
