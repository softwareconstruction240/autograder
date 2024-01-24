package edu.byu.cs.model;

import java.time.Instant;

public record QueueItem(
        String netId,
        Phase phase,
        Instant timeAdded,
        boolean started
) {

}
