package edu.byu.cs.autograder;

import edu.byu.cs.controller.WebSocketController;
import edu.byu.cs.dataAccess.DaoService;
import edu.byu.cs.model.QueueItem;
import org.eclipse.jetty.websocket.api.Session;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Controller for handling the queue of graders
 */
public class TrafficController {

    /**
     * A map of netIds to sessions that are subscribed to updates for that netId
     */
    public static final ConcurrentHashMap<String, List<Session>> sessions = new ConcurrentHashMap<>();

    /**
     * The executor service that runs the graders
     */
    private final ExecutorService executorService = Executors.newFixedThreadPool(1);

    private static final TrafficController trafficController = new TrafficController();

    private TrafficController() {
    }

    public static TrafficController getInstance() {
        return trafficController;
    }

    /**
     * Broadcasts the current queue status to all connected clients.
     * Each client will be notified of their specific position in the queue.
     */
    public static void broadcastQueueStatus() {

        Collection<String> netIdsInQueue = DaoService.getQueueDao().getAll().stream()
                .filter(queueItem -> !queueItem.started())
                .map(QueueItem::netId).toList();

        int i = 1;
        for (String netId : netIdsInQueue) {
            getInstance().notifySubscribers(netId, Map.of(
                    "type", "queueStatus",
                    "position", i,
                    "total", netIdsInQueue.size()
            ));
            i++;
        }
    }

    /**
     * Adds a grader to the queue. The grader will be run when there is an available thread
     *
     * @param grader the grader to add
     */
    public void addGrader(Grader grader) {
        executorService.submit(grader);
    }


    public void notifySubscribers(String netId, Map<String, Object> message) {
        List<Session> sessionList = sessions.get(netId);

        if (sessionList == null) return;
        for (Session session : sessionList) {
            if (session.isOpen()) {
                WebSocketController.send(session, message);
            }
        }
    }
}
