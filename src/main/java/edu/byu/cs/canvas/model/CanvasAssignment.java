package edu.byu.cs.canvas.model;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * Represents a Canvas assignment
 *
 * @param id the id of the assignment in Canvas
 * @param name the name of the assignment in Canvas
 * @param dueAt the date the assignment is due
 * @param rubric the rubric for the assignment
 */
public record CanvasAssignment(
        Integer id,
        String name,
        ZonedDateTime dueAt,
        List<CanvasRubric> rubric
) {
    /**
     * Contains Canvas information for a rubric item. Note the difference between the {@code CanvasRubric} and
     * the {@link CanvasRubricItem} that the AutoGrader creates to score a rubric item for a submission.
     *
     * @param id the id of the rubric item stored internally in Canvas (generally something like '_1234').
     *           This is stored in the rubric_config table in the autograder under the 'rubric_id' column.
     * @param points the amount of possible points for the rubric
     * @param description the name of the rubric item, for example, 'web api works'. The reason it's called
     *                    description is because of the json sent from Canvas.
     */
    public record CanvasRubric(
            String id,
            Integer points,
            String description
    ) {}
}
