package edu.byu.cs.model;

import java.util.List;

public record RubricConfig(
        List<RubricConfigItem> rubricItems
) {
    public record RubricConfigItem(
            Phase phase,
            Rubric.RubricType type,
            int points,
            String description
    ) {
    }
}
