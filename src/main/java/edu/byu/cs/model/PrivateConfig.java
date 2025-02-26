package edu.byu.cs.model;

import java.util.ArrayList;
import java.util.Map;

public record PrivateConfig(
    PenaltyConfig penalty,
    int courseNumber,
    ArrayList<AssignmentConfig> assignments
) {
    public record PenaltyConfig(
        float perDayLatePenalty,
        float gitCommitPenalty,
        int maxLateDaysPenalized,
        int linesChangedPerCommit,
        int clockForgivenessMinutes
    ){ }

    public record AssignmentConfig(
            Phase phase,
            int assignmentId,
            Map<Rubric.RubricType, RubricConfig.RubricConfigItem> rubricItems
    ){ }
}
