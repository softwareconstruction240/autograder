package edu.byu.cs.model;

import java.time.Instant;

/**
 * Represents an item in the queue to be graded
 *
 * @param netId the netId of the student
 * @param phase the phase to be graded
 * @param timeAdded the time the item was added to the queue
 * @param started a boolean indicating whether the item has started the grading process
 */
public record QueueItem(
        String netId,
        Phase phase,
        Instant timeAdded,
        boolean started
) { }
