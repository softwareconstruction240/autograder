package edu.byu.cs.server;

import edu.byu.cs.controller.WebSocketController;

import static edu.byu.cs.controller.security.JwtUtils.validateToken;
import static spark.Spark.*;

public class Server{

    private static final String ALL_PASS_REPO = "https://github.com/pawlh/chess-passing.git";
    private static final String ALL_FAIL_REPO = "https://github.com/softwareconstruction240/chess.git";
    public static void main(String[] args) {

        port(8080);

        webSocket("/ws", WebSocketController.class);

        staticFiles.location("/public");

        path("/api", () -> {
            before("/*", (req, res) -> {
                String token = req.cookie("token");
                if (token == null || !validateToken(token)) {
                    res.redirect("/login", 302);
                    halt(401);
                }
            });
        });
        init();
    }
}