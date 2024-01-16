package edu.byu.cs.controller;

import com.google.gson.Gson;
import edu.byu.cs.autograder.TrafficController;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@WebSocket
public class WebSocketController {
    private static final Logger LOGGER = LoggerFactory.getLogger(WebSocketController.class);

    @OnWebSocketConnect
    public void onConnect(Session session) {}

    @OnWebSocketClose
    public void onClose(Session session, int statusCode, String reason) {
    }

    @OnWebSocketError
    public void onError(Session session, Throwable t) {
        LOGGER.error("WebSocket error: ", t);
    }

    @OnWebSocketMessage
    public void onMessage(Session session, String message) {
        String netId = null;
        try {
            netId = JwtUtils.validateToken(message);
        } catch (Exception e) {
            LOGGER.warn("Exception thrown while validating token: ", e);

            sendError(session, "Invalid token");
            session.close();
            return;
        }

        if (!TrafficController.sessions.containsKey(netId)) {
            sendError(session, "You are not in the queue");
            session.close();
            return;
        }

        if (TrafficController.sessions.get(netId).contains(session))
            return;

        TrafficController.sessions.get(netId).add(session);
        TrafficController.broadcastQueueStatus();
    }

    /**
     * Sends a message to the given session
     *
     * @param session the session to send the message to
     * @param message the message
     */
    public static void send(Session session, Map<String, Object> message) {
        String jsonMessage = new Gson().toJson(message);
        try {
            session.getRemote().sendString(jsonMessage);
        } catch (Exception e) {
            LOGGER.warn("Exception thrown while sending: ", e);
        }
    }

    /**
     * Sends an error message to the given session
     *
     * @param session the session to send the message to
     * @param message the error message
     */
    public static void sendError(Session session, String message) {
        send(
                session,
                Map.of(
                        "type", "error",
                        "message", message
                ));
    }


}
