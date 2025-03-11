package edu.byu.cs.model;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Represents the config table that contains information that only admins can see
 *
 * @param penalty A {@link PenaltyConfig} containing information about penalties
 *                students may receive
 * @param courseNumber The number assigned to the course on Canvas
 * @param assignments A list containing information about an assignment in
 *                    Canvas for each {@link AssignmentConfig}.
 * @param holidays A list of holidays the AutoGrader doesn't count towards the late penalty
 */
public record PrivateConfig(
    PenaltyConfig penalty,
    int courseNumber,
    List<AssignmentConfig> assignments,
    String[] holidays
) {
    /**
     * Represents the configuration information needed for penalties students may receive
     *
     * @param perDayLatePenalty The penalty to apply on a submission for everyday late
     * @param gitCommitPenalty the penalty to apply on a submission for missing commits
     * @param maxLateDaysPenalized the maximum number of days the late penalty should apply
     * @param linesChangedPerCommit the minimum number of lines needed for a commit to count
     * @param clockForgivenessMinutes the number of minutes a commit can be authored
     *                                past the time of submission
     */
    public record PenaltyConfig(
        float perDayLatePenalty,
        float gitCommitPenalty,
        int maxLateDaysPenalized,
        int linesChangedPerCommit,
        int clockForgivenessMinutes
    ){ }

    /**
     * Represents the configuration information for a Canvas assignment
     *
     * @param phase the phase associated with the Canvas assignment
     * @param assignmentId the id of the assignment in Canvas
     * @param rubricItems the rubric items for the Canvas assignment
     */
    public record AssignmentConfig(
            Phase phase,
            int assignmentId,
            Map<Rubric.RubricType, RubricConfig.RubricConfigItem> rubricItems
    ){ }
}
