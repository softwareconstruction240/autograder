package edu.byu.cs.canvas;

/**
 * Represents the rubric for a Canvas assignment. Some rubrics may have null values for some fields.
 *
 * @param passoffTests
 * @param unitTests
 * @param quality
 */
public record Rubric(
        RubricItem passoffTests,
        RubricItem unitTests,
        RubricItem quality
) {

    record RubricItem(
            String description,
            String notes,
            Float score

    ) {
    }
}
