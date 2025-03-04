package edu.byu.cs.model;

import java.util.EnumMap;

/**
 * Represents the configuration of rubric items for a single phase
 *
 * @param phase the phase
 * @param items the items for configuration
 */
public record RubricConfig(Phase phase, EnumMap<Rubric.RubricType, RubricConfigItem> items) {

    /**
     * Represents the configuration for a rubric item
     *
     * @param category the category for the rubric item
     * @param criteria the criteria needed to pass for the rubric item
     * @param points the amount of points for the rubric item
     * @param rubric_id the rubric id for the rubric item
     */
    public record RubricConfigItem(String category, String criteria, int points, String rubric_id) {}

}
