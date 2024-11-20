package edu.byu.cs.model;

import edu.byu.cs.canvas.model.CanvasAssignment;

import java.util.ArrayList;
import java.util.Map;

public record PrivateConfig(
    PenaltyConfig penalty,
    ArrayList<CourseConfig> courses
) {
    public record PenaltyConfig(
        float perDayLatePenalty,
        float gitCommitPenalty,
        int maxLateDaysPenalized,
        int linesChangedPerCommit,
        int clockForgivenessMinutes
    ){}

    public record CourseConfig(
        int courseNumber,
        ArrayList<AssignmentConfig> assignments
    ){
        public record AssignmentConfig(
                Phase phase,
                int assignmentId,
                Map<Rubric.RubricType, RubricConfig.RubricConfigItem> rubricItems
        ){}
    }
}
