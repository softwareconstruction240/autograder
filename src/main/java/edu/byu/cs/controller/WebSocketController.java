package edu.byu.cs.controller;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import edu.byu.cs.autograder.*;
import edu.byu.cs.controller.netmodel.GradeRequest;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

@WebSocket
public class WebSocketController {

    private static final ConcurrentLinkedQueue<Session> queue = new ConcurrentLinkedQueue<>();

    @OnWebSocketConnect
    public void onConnect(Session session) { }

    @OnWebSocketClose
    public void onClose(Session session, int statusCode, String reason) {
        if (queue.remove(session))
            broadcastQueueStatus();
    }

    @OnWebSocketError
    public void onError(Session session, Throwable t) {
        System.out.println("WebSocket error: ");
        if (session.isOpen() && queue.remove(session))
            broadcastQueueStatus();
        t.printStackTrace();
    }

    @OnWebSocketMessage
    public void onMessage(Session session, String message) {
        if (queue.contains(session)) {
            sendError(session, "You are already in the queue");
            return;
        }

        GradeRequest request;
        try {
            request = new Gson().fromJson(message, GradeRequest.class);
        } catch (JsonSyntaxException e) {
            sendError(session, "Request must be valid json");
            return;
        }

        // FIXME: improve git url validation
        if (!request.repoUrl().matches("^https://[\\w.]+.\\w+/[\\w\\D]+/[\\w-/]+.git$")) {
            sendError(session, "That doesn't look like a valid git url");
            return;
        }
        if (!Arrays.asList(0, 1, 3, 4, 6).contains(request.phase())) {
            sendError(session, "Valid phases are 0, 1, 3, 4, or 6");
            return;
        }

        queue.add(session);
        send(
                session,
                Map.of(
                        "type", "queueStatus",
                        "position", queue.size(),
                        "total", queue.size()
                )
        );

        try {
            Grader grader = getGrader(session, request);

            TrafficController.getInstance().addGrader(grader);

        } catch (Exception e) {
            sendError(session, "Something went wrong");
        }


    }

    /**
     * Creates a grader for the given request with an observer that sends messages to the given session
     *
     * @param session the session to send messages to
     * @param request the request to create a grader for
     * @return the grader
     * @throws IOException if there is an error creating the grader
     */
    private Grader getGrader(Session session, GradeRequest request) throws IOException {
        Grader.Observer observer = new Grader.Observer() {
            @Override
            public void notifyStarted() {
                queue.remove(session);
                send(
                        session,
                        Map.of(
                                "type", "started"
                        ));
                broadcastQueueStatus();
            }

            @Override
            public void update(String message) {
                send(
                        session,
                        Map.of(
                                "type", "update",
                                "message", message
                        ));
            }

            @Override
            public void notifyError(String message) {
                sendError(session, message);
            }

            @Override
            public void notifyDone(TestAnalyzer.TestNode results) {
                send(
                        session,
                        Map.of(
                                "type", "results",
                                "results", new Gson().toJson(results)
                        ));
            }
        };

        return switch (request.phase()) {
            case 0 -> new PhaseZeroGrader(request.repoUrl(), observer);
            case 1 -> new PhaseOneGrader(request.repoUrl(), observer);
            case 3 -> null;
            case 4 -> null;
            case 6 -> null;
            default -> throw new IllegalStateException("Unexpected value: " + request.phase());
        };
    }

    /**
     * Sends a message to the given session
     *
     * @param session the session to send the message to
     * @param message the message
     */
    private void send(Session session, Map<String, Object> message) {
        try {
            session.getRemote().sendString(new Gson().toJson(message));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Sends an error message to the given session
     *
     * @param session the session to send the message to
     * @param message the error message
     */
    private void sendError(Session session, String message) {
        send(
                session,
                Map.of(
                        "type", "error",
                        "message", message
                ));
    }

    /**
     * Broadcasts the current queue status to all connected clients.
     * Each client will be notified of their specific position in the queue.
     */
    private void broadcastQueueStatus() {
        int i = 1;
        for (Session session : queue) {
            send(
                    session,
                    Map.of(
                            "type", "queueStatus",
                            "position", i,
                            "total", queue.size()
                    ));
            i++;
        }
    }
}
