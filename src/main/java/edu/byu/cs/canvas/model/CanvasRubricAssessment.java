package edu.byu.cs.canvas.model;

import java.util.Map;

public record CanvasRubricAssessment(Map<String, CanvasSubmissionRubricItem> items) {}
