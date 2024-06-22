package edu.byu.cs.model;

import java.util.EnumMap;

public record RubricConfig(

        Phase phase,
        EnumMap<Rubric.RubricType, RubricConfigItem> items
) {
    public record RubricConfigItem(
            String category,
            String criteria,
            int points
    ) {
    }
}
