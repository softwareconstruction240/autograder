package edu.byu.cs.autograder;

import edu.byu.cs.controller.WebSocketController;
import org.eclipse.jetty.websocket.api.Session;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Controller for handling the queue of graders
 */
public class TrafficController {
    /**
     * A queue of netIds that are waiting to be graded
     */
    public static final ConcurrentLinkedQueue<String> queue = new ConcurrentLinkedQueue<>();
    /**
     * A map of netIds to sessions that are subscribed to updates for that netId
     */
    public static final ConcurrentHashMap<String, List<Session>> sessions = new ConcurrentHashMap<>();

    /**
     * The executor service that runs the graders
     */
    private final ExecutorService executorService = Executors.newFixedThreadPool(1);

    private static final TrafficController trafficController = new TrafficController();

    public static TrafficController getInstance() {
        return trafficController;
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
        sessions.get(netId).stream()
                .filter(Session::isOpen)
                .forEach(session -> WebSocketController.send(session, message));
    }
}
