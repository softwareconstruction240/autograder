package edu.byu.cs.canvas.model;

/**
 * Represents a submission to be sent to Canvas
 *
 * @param url the url to the student's repository used for the submission
 * @param rubric_assessment the results of the rubric items for the submission
 * @param score the total score for the submission
 */
public record CanvasSubmission(String url, CanvasRubricAssessment rubric_assessment, Float score) {}
