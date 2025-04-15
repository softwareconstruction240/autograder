package edu.byu.cs.canvas.model;

/**
 * The item the AutoGrader creates for scoring and storing a rubric item from a submission for Canvas
 *
 * @param comments any comments about the scoring of a submission for the rubric item
 * @param points the amount of points a submission will receive for the rubric item
 */
public record CanvasRubricItem(String comments, float points) {}
