package edu.byu.cs.server;

import edu.byu.cs.controller.WebSocketController;

import static edu.byu.cs.controller.AuthController.verifyAuthenticatedMiddleware;
import static edu.byu.cs.controller.CasController.*;
import static spark.Spark.*;

public class Server{

    private static final String ALL_PASS_REPO = "https://github.com/pawlh/chess-passing.git";
    private static final String ALL_FAIL_REPO = "https://github.com/softwareconstruction240/chess.git";
    public static void main(String[] args) {

        port(8080);

        webSocket("/ws", WebSocketController.class);

        staticFiles.location("/public");

        get("/callback", callbackGet);
        get("/login", loginGet);
        get("/logout", logoutGet);

        path("/api", () -> {
            before("/*", verifyAuthenticatedMiddleware);
        });
        init();
    }
}