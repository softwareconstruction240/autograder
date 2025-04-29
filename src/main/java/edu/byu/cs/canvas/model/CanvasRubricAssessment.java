package edu.byu.cs.canvas.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Holds a map of rubric items, mapped by their ids
 *
 * @param items the map of rubric items
 */
public record CanvasRubricAssessment(Map<String, CanvasRubricItem> items) {
    /**
     * Inserts a {@link CanvasRubricItem} into the {@code CanvasRubricAssessment}
     *
     * @param itemId the rubric id for the rubric item
     * @param rubricItem the rubric item being scored
     */
    public void insertItem(String itemId, CanvasRubricItem rubricItem) {
        items.put(itemId, rubricItem);
    }

    @Override
    public CanvasRubricAssessment clone() {
        return new CanvasRubricAssessment(new HashMap<>(items));
    }
}
