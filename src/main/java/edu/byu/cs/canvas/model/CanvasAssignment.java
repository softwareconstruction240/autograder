package edu.byu.cs.canvas.model;

import java.time.ZonedDateTime;
import java.util.List;

public record CanvasAssignment(Integer id, String name, ZonedDateTime due_at, List<CanvasAssignmentRubricItem> rubric) { }
