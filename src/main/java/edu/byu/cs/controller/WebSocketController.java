package edu.byu.cs.controller;

import edu.byu.cs.dataAccess.DaoService;
import edu.byu.cs.dataAccess.DataAccessException;
import edu.byu.cs.util.JwtUtils;
import edu.byu.cs.util.Serializer;
import org.eclipse.jetty.websocket.api.CloseException;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@WebSocket
public class WebSocketController {
    private static final Logger LOGGER = LoggerFactory.getLogger(WebSocketController.class);

    @OnWebSocketError
    public void onError(Session session, Throwable t) {
        if (!(t instanceof CloseException))
            LOGGER.error("WebSocket error: ", t);
    }

    @OnWebSocketMessage
    public void onMessage(Session session, String message) {
        String netId;
        try {
            netId = JwtUtils.validateToken(message);
        } catch (Exception e) {
            LOGGER.warn("Exception thrown while validating token: ", e);

            sendError(session, "Invalid token");
            session.close();
            return;
        }

        try {
            if (!DaoService.getQueueDao().isAlreadyInQueue(netId)) {
                sendError(session, "You are not in the queue");
                session.close();
                return;
            }
        } catch (DataAccessException e) {
            LOGGER.error("Error accessing queue", e);
            throw new RuntimeException("Error accessing queue", e);
        }

        if (TrafficController.hasSession(netId, session))
            return;

        TrafficController.addSession(netId, session);
        try {
            TrafficController.broadcastQueueStatus();
        } catch (DataAccessException e) {
            LOGGER.error("Error broadcasting queue status", e);
            throw new RuntimeException("Error broadcasting queue status", e);
        }
    }

    /**
     * Sends a message to the given session
     *
     * @param session the session to send the message to
     * @param message the message
     */
    public static void send(Session session, Map<String, Object> message) {
        String jsonMessage = Serializer.serialize(message);
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
