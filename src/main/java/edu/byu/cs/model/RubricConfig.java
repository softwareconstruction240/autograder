package edu.byu.cs.model;

public record RubricConfig(

        Phase phase,
        RubricConfigItem passoffTests,
        RubricConfigItem unitTests,
        RubricConfigItem quality,
        RubricConfigItem gitCommits
) {
    public record RubricConfigItem(
            String category,
            String criteria,
            int points
    ) {
    }
}
