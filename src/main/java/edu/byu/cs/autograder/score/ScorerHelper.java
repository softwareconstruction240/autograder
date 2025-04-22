package edu.byu.cs.autograder.score;

import edu.byu.cs.autograder.GradingException;
import edu.byu.cs.dataAccess.DaoService;
import edu.byu.cs.dataAccess.DataAccessException;
import edu.byu.cs.model.QueueItem;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * A helper class used while processing a submission and during scoring
 */
public class ScorerHelper {
    /**
     * Returns the HandInDate of the provided student as an {@link Instant}.
     * This should only be used while processing a submission from a student.
     *
     * @param netId The student to look up
     * @return The instant the queue entry was added
     * @throws GradingException If no queue item exists for the student
     */
    public static Instant getHandInDateInstant(String netId) throws GradingException, DataAccessException {
        QueueItem studentQueueItem = DaoService.getQueueDao().get(netId);
        if (studentQueueItem == null) {
            throw new GradingException("Cannot resolve hand in date without student queue item");
        }
        return studentQueueItem.timeAdded();
    }

    /**
     * Returns the HandInDate of the provided student as a {@link ZonedDateTime}.
     * @see ScorerHelper#getHandInDateZoned(String)
     *
     * @param netId The student to look up
     * @return The {@link ZonedDateTime} representing the hand in date
     * @throws GradingException if no queue item exists for the student
     */
    public static ZonedDateTime getHandInDateZoned(String netId) throws GradingException, DataAccessException {
        // TODO: Read in this timezone from a dynamic location. See #156.
        return getHandInDateInstant(netId).atZone(ZoneId.of("America/Denver"));
    }
}
