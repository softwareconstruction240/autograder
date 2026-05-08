package edu.byu.cs.canvas.model;

import com.google.gson.annotations.SerializedName;

/**
 * Represents a submission to be sent to Canvas
 *
 * @param url the url to the student's repository used for the submission
 * @param rubricAssessment the results of the rubric items for the submission
 * @param score the total score for the submission
 */
public record CanvasSubmission(
        String url,
        @SerializedName("rubric_assessment") CanvasRubricAssessment rubricAssessment,
        Float score
) {}
