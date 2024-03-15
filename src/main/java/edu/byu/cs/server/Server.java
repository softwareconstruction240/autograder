package edu.byu.cs.server;

import edu.byu.cs.controller.SubmissionController;
import edu.byu.cs.controller.WebSocketController;
import edu.byu.cs.properties.ApplicationProperties;
import edu.byu.cs.util.ResourceUtils;
import org.apache.commons.cli.*;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import static edu.byu.cs.controller.AdminController.*;
import static edu.byu.cs.controller.AuthController.*;
import static edu.byu.cs.controller.CasController.*;
import static edu.byu.cs.controller.SubmissionController.*;
import static spark.Spark.*;

public class Server {

    public static void setupEndpoints() {
        port(8080);

        webSocket("/ws", WebSocketController.class);
        webSocketIdleTimeoutMillis(300000);

        staticFiles.location("/frontend/dist");

        before((request, response) -> {
            response.header("Access-Control-Allow-Headers", "Authorization,Content-Type");
            response.header("Access-Control-Allow-Methods", "GET,PUT,POST,DELETE,PATCH,OPTIONS");
            response.header("Access-Control-Allow-Credentials", "true");
            response.header("Access-Control-Allow-Origin", ApplicationProperties.frontendUrl());
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
    }

    private static void setupProperties(String[] args) {
        Options options = getOptions();

        Properties properties = new Properties();

        CommandLineParser parser = new DefaultParser();
        try {
            CommandLine cmd = parser.parse(options, args);
            if (cmd.hasOption("db-url")) {
                properties.setProperty("db-url", cmd.getOptionValue("db-url"));
            }
            if (cmd.hasOption("db-name")) {
                properties.setProperty("db-name", cmd.getOptionValue("db-name"));
            }
            if (cmd.hasOption("db-user")) {
                properties.setProperty("db-user", cmd.getOptionValue("db-user"));
            }
            if (cmd.hasOption("db-pass")) {
                properties.setProperty("db-pass", cmd.getOptionValue("db-pass"));
            }
            if (cmd.hasOption("frontend-url")) {
                properties.setProperty("frontend-url", cmd.getOptionValue("frontend-url"));
            }
            if (cmd.hasOption("cas-callback-url")) {
                properties.setProperty("cas-callback-url", cmd.getOptionValue("cas-callback-url"));
            }
            if (cmd.hasOption("canvas-token")) {
                properties.setProperty("canvas-token", cmd.getOptionValue("canvas-token"));
            }
            if (cmd.hasOption("student-db-host")) {
                properties.setProperty("student-db-host", cmd.getOptionValue("student-db-host"));
            }
            if (cmd.hasOption("student-db-port")) {
                properties.setProperty("student-db-port", cmd.getOptionValue("student-db-port"));
            }
            if (cmd.hasOption("student-db-user")) {
                properties.setProperty("student-db-user", cmd.getOptionValue("student-db-user"));
            }
            if (cmd.hasOption("student-db-pass")) {
                properties.setProperty("student-db-pass", cmd.getOptionValue("student-db-pass"));
            }
        } catch (ParseException e) {
            throw new RuntimeException("Error parsing command line arguments", e);
        }

        ApplicationProperties.loadProperties(properties);
    }

    private static Options getOptions() {
        Options options = new Options();
        options.addOption(null, "db-url", true, "Database URL");
        options.addOption(null, "db-name", true, "Database Name");
        options.addOption(null, "db-user", true, "Database User");
        options.addOption(null, "db-pass", true, "Database Password");
        options.addOption(null, "frontend-url", true, "Frontend URL");
        options.addOption(null, "cas-callback-url", true, "CAS Callback URL");
        options.addOption(null, "canvas-token", true, "Canvas Token");
        return options;
    }


    public static void main(String[] args) {
        ResourceUtils.copyResourceFiles("phases", new File(""));
        setupProperties(args);
        setupEndpoints();

        try {
            SubmissionController.reRunSubmissionsInQueue();
        } catch (IOException e) {
            throw new RuntimeException("Error rerunning submissions already in queue");
        }
    }
}