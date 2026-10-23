package edu.byu.cs.autograder;

import edu.byu.cs.controller.TrafficController;
import edu.byu.cs.dataAccess.DataAccessException;
import edu.byu.cs.dataAccess.daoInterface.QueueDao;
import edu.byu.cs.model.Submission;
import edu.byu.cs.util.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * The implementation of the {@link GradingObserver} interface. This notifies the user
 * through the use of the {@link TrafficController}.
 */
public class GradingObserverImpl implements GradingObserver {

    private static final Logger LOGGER = LoggerFactory.getLogger(GradingObserverImpl.class);

    private final String netId;
    private final QueueDao queueDao;

    public GradingObserverImpl(String netId, QueueDao queueDao) {
        this.netId = netId;
        this.queueDao = queueDao;
    }

    @Override
    public void notifyStarted() {
        try {
            queueDao.markStarted(netId);
        } catch (DataAccessException e) {
            LOGGER.error("Error marking queue item as started", e);
            return;
        }

        notifySubscribers(Map.of("type", "started"));

        try {
            TrafficController.broadcastQueueStatus(queueDao);
        } catch (Exception e) {
            LOGGER.error("Error broadcasting queue status", e);
        }
    }

    @Override
    public void update(String message) {
        notifySubscribers(Map.of("type", "update", "message", message));
    }

    @Override
    public void notifyError(String message) {
        notifyError(message, Map.of());
    }

    @Override
    public void notifyError(String message, Submission submission) {
        notifyError(message, Map.of("results", Serializer.serialize(submission)));
    }

    private void notifyError(String message, Map<String, Object> contents) {
        contents = new HashMap<>(contents);
        contents.put("type", "error");
        contents.put("message", message);
        notifySubscribers(contents);
        removeFromQueue();
    }

    @Override
    public void notifyWarning(String message) {
        notifySubscribers(Map.of("type", "warning", "message", message));
    }

    @Override
    public void notifyDone(Submission submission) {
        notifySubscribers(Map.of("type", "results", "results", Serializer.serialize(submission)));
        removeFromQueue();
    }

    private void notifySubscribers(Map<String, Object> contents) {
        try {
            TrafficController.getInstance().notifySubscribers(netId, contents);
        } catch (Exception e) {
            LOGGER.error("Error updating subscribers", e);
        }
    }

    private void removeFromQueue() {
        TrafficController.clearSessions(netId);
        try {
            queueDao.remove(netId);
        } catch (DataAccessException e) {
            LOGGER.error("Error removing queue item", e);
        }
    }
}
