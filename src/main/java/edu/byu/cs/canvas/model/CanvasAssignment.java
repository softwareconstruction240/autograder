package edu.byu.cs.canvas.model;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * Represents a Canvas assignment
 *
 * @param id the id of the assignment in Canvas
 * @param name the name of the assignment in Canvas
 * @param due_at the date the assignment is due
 * @param rubric the rubric for the assignment
 */
public record CanvasAssignment(
        Integer id,
        String name,
        ZonedDateTime due_at,
        List<CanvasRubric> rubric
) {
    /** A rubric item for a canvas assignment
     *
     * @param id the id of the rubric item stored internally in Canvas
     *           (generally something like '_1234')
     * @param points the amount of possible points for the rubric
     * @param description a description of the rubric item
     *                    (generally something like 'Passes passoff test cases')
     */
    public record CanvasRubric(
            String id,
            Integer points,
            String description
    ) {}
}
