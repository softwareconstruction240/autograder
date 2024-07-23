package edu.byu.cs.model;

import edu.byu.cs.canvas.model.CanvasAssignment;

import java.util.EnumMap;
import java.util.Map;

public record SetCourseIdsRequest(
        int courseNumber,
        EnumMap<Phase, Integer> assignmentIds,
        EnumMap<Phase, Map<Rubric.RubricType, CanvasAssignment.CanvasRubric>> rubricInfo
) { }
