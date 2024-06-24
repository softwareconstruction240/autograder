package edu.byu.cs.canvas.model;

import java.util.HashMap;
import java.util.Map;

public record CanvasRubricAssessment(Map<String, CanvasRubricItem> items) {
    public void insertItem(String itemId, CanvasRubricItem rubricItem) {
        items.put(itemId, rubricItem);
    }

    @Override
    public CanvasRubricAssessment clone() {
        return new CanvasRubricAssessment(new HashMap<>(items));
    }
}
