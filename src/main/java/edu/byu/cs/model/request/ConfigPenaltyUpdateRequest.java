package edu.byu.cs.model.request;

public record ConfigPenaltyUpdateRequest(
        float perDayLatePenalty,
        int maxLateDaysPenalized,
        float gitCommitPenalty,
        int linesChangedPerCommit,
        int clockForgivenessMinutes
) {}
