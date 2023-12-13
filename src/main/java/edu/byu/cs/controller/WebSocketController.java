package edu.byu.cs.controller;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import edu.byu.cs.autograder.Grader;
import edu.byu.cs.autograder.PhaseOneGrader;
import edu.byu.cs.autograder.TestAnalyzer;
import edu.byu.cs.autograder.TrafficController;
import edu.byu.cs.controller.netmodel.GradeRequest;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

import static spark.Spark.webSocket;

@WebSocket
public class WebSocketController {

    public static void registerRoute() {
        webSocket("/ws", WebSocketController.class);
    }

    @OnWebSocketConnect
    public void onConnect(Session session) {
        System.out.println("WebSocket connected");
    }

    @OnWebSocketClose
    public void onClose(Session session, int statusCode, String reason) {
        System.out.println("WebSocket closed");
    }

    @OnWebSocketError
    public void onError(Throwable t) {
        System.out.println("WebSocket error");
        t.printStackTrace();
    }

    @OnWebSocketMessage
    public void onMessage(Session session, String message) {
        System.out.println("WebSocket message: " + message);

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

        try {
            Grader grader = getGrader(session, request);

            TrafficController.getInstance().addGrader(grader);

        } catch (Exception e) {
            sendError(session, "Something went wrong");
        }


    }

    private Grader getGrader(Session session, GradeRequest request) throws IOException {
        Grader.Observer observer = new Grader.Observer() {
            @Override
            public void update(String message) {
                send(session, "message", message);
            }

            @Override
            public void notifyError(String message) {
                sendError(session, message);
            }

            @Override
            public void notifyDone(TestAnalyzer.TestNode results) {
                send(session, "results", new Gson().toJson(results));
            }
        };

        return switch (request.phase()) {
            case 0 -> null;
            case 1 -> new PhaseOneGrader(request.repoUrl(), "./stage", observer);
            case 3 -> null;
            case 4 -> null;
            case 6 -> null;
            default -> throw new IllegalStateException("Unexpected value: " + request.phase());
        };
    }

    private void send(Session session, String type, String message) {
        try {
            session.getRemote().sendString(new Gson().toJson(Map.of(
                    "type", type,
                    "message", message
            )));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendError(Session session, String message) {
        send(session, "error", message);
    }
}
