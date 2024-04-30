package edu.byu.cs.model;

public record RubricConfig(

        Phase phase,
        RubricConfigItem passoffTests,
        RubricConfigItem unitTests,
        RubricConfigItem quality
) {
    public record RubricConfigItem(
            String category,
            String criteria,
            int points,
            String rubricId
    ) {
    }
}
