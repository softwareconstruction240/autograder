package edu.byu.cs.server;

import edu.byu.cs.controller.WebSocketController;
import edu.byu.cs.properties.ConfigProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static edu.byu.cs.controller.AuthController.*;
import static edu.byu.cs.controller.CasController.*;
import static edu.byu.cs.controller.SubmissionController.submissionXGet;
import static edu.byu.cs.controller.SubmissionController.submitPost;
import static spark.Spark.*;

public class Server {

    private static final Logger LOGGER = LoggerFactory.getLogger(Server.class);

    public static void main(String[] args) {

        port(8080);

        webSocket("/ws", WebSocketController.class);

        staticFiles.location("/public");

        before((request, response) -> {
            response.header("Access-Control-Allow-Headers", "Authorization,Content-Type");
            response.header("Access-Control-Allow-Methods", "GET,PUT,POST,DELETE,OPTIONS");
            response.header("Access-Control-Allow-Credentials", "true");
            response.header("Access-Control-Allow-Origin", ConfigProperties.frontendAppUrl());
        });


        path("/auth", () -> {
            get("/callback", callbackGet);
            get("/login", loginGet);

            // all routes after this point require authentication
            post("/register", registerPost);
            get("/logout", logoutGet);
        });

        path("/api", () -> {
            before("/*", verifyAuthenticatedMiddleware);

            post("/submit", submitPost);

            get("/submission/:phase", submissionXGet);

            get("/me", meGet);
        });
        before((request, response) -> {
            LOGGER.info("Received from " + request.ip() + ":\n" + request.body());
        });
        afterAfter((request, response) -> {
            LOGGER.info("Sent to " + request.ip() + ":\n" + response.body());
        });
        init();
    }
}