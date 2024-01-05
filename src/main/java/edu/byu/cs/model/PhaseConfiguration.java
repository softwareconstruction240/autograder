package edu.byu.cs.model;

import java.time.ZonedDateTime;

/**
 * Represents a phase configuration. Contains information unique to a phase such as the due date.
 *
 * @param phase
 * @param dueDate
 */
public record PhaseConfiguration(
        Phase phase,
        ZonedDateTime dueDate

) {
}
