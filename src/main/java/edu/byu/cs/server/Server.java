package edu.byu.cs.server;

import edu.byu.cs.controller.WebSocketController;

import static edu.byu.cs.controller.AuthController.*;
import static edu.byu.cs.controller.CasController.*;
import static edu.byu.cs.controller.SubmissionController.submissionXGet;
import static spark.Spark.*;

public class Server{

    private static final String ALL_PASS_REPO = "https://github.com/pawlh/chess-passing.git";
    private static final String ALL_FAIL_REPO = "https://github.com/softwareconstruction240/chess.git";
    public static void main(String[] args) {

        port(8080);

        webSocket("/ws", WebSocketController.class);

        staticFiles.location("/public");


        path("/auth", () -> {
            get("/callback", callbackGet);
            get("/login", loginGet);

            // all routes after this point require authentication
            post("/register", registerPost);
            get("/logout", logoutGet);
        });

        path("/api", () -> {
            before("/*", verifyAuthenticatedMiddleware);

            get("/submission/:phase", submissionXGet);

            get("/me", meGet);
        });
        init();
    }
}