package edu.byu.cs.server;

import edu.byu.cs.controller.SubmissionController;
import edu.byu.cs.controller.WebSocketController;
import edu.byu.cs.properties.ConfigProperties;

import java.io.IOException;

import static edu.byu.cs.controller.AdminController.*;
import static edu.byu.cs.controller.AuthController.*;
import static edu.byu.cs.controller.CasController.*;
import static edu.byu.cs.controller.SubmissionController.*;
import static spark.Spark.*;

public class Server {


    public static void main(String[] args) {

        port(8080);

        webSocket("/ws", WebSocketController.class);
        webSocketIdleTimeoutMillis(300000);

        staticFiles.location("/frontend/dist");

        before((request, response) -> {
            response.header("Access-Control-Allow-Headers", "Authorization,Content-Type");
            response.header("Access-Control-Allow-Methods", "GET,PUT,POST,DELETE,PATCH,OPTIONS");
            response.header("Access-Control-Allow-Credentials", "true");
            response.header("Access-Control-Allow-Origin", ConfigProperties.frontendAppUrl());
        });

        path("/auth", () -> {
            get("/callback", callbackGet);
            get("/login", loginGet);

            // all routes after this point require authentication
            post("/register", registerPost);
            post("/logout", logoutPost);
        });

        path("/api", () -> {
            before("/*", (req, res) -> {
                if (!req.requestMethod().equals("OPTIONS"))
                    verifyAuthenticatedMiddleware.handle(req, res);
            });

            get("/submit", submitGet);
            post("/submit", submitPost);

            get("/submission/:phase", submissionXGet);

            get("/me", meGet);

            path("/admin", () -> {
                before("/*", (req, res) -> {
                    if (!req.requestMethod().equals("OPTIONS"))
                        verifyAdminMiddleware.handle(req, res);
                });

                get("/users", usersGet);

                patch("/user/:netId", userPatch);

                get("/submissions/latest", latestSubmissionsGet);

                get("/submissions/latest/:count", latestSubmissionsGet);

                get("/test_mode", testModeGet);

                get("/submissions/active", submissionsActiveGet);

                get("/submissions/student/:netID", studentSubmissionsGet);

                post("/submissions/rerun", submissionsReRunPost);

                get("/analytics/commit", commitAnalyticsGet);

                get("/analytics/commit/:option", commitAnalyticsGet);

                get("/honorChecker/zip/:section", honorCheckerZipGet);
            });
        });

        // spark's notFound method does not work
        get("/*", (req, res) -> {
            if (req.pathInfo().equals("/ws"))
                return null;

            String urlParms = req.queryString();
            urlParms = urlParms == null ? "" : "?" + urlParms;
            res.redirect("/" + urlParms, 302);
            return null;
        });
        init();

        try {
            SubmissionController.reRunSubmissionsInQueue();
        } catch (IOException e) {
            throw new RuntimeException("Error rerunning submissions already in queue");
        }
    }
}