package edu.byu.cs.server;

import edu.byu.cs.controller.SubmissionController;
import edu.byu.cs.controller.WebSocketController;
import edu.byu.cs.dataAccess.DaoService;
import edu.byu.cs.dataAccess.DataAccessException;
import edu.byu.cs.dataAccess.sql.*;
import edu.byu.cs.properties.ApplicationProperties;
import edu.byu.cs.util.ResourceUtils;
import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import static edu.byu.cs.controller.AdminController.*;
import static edu.byu.cs.controller.AuthController.*;
import static edu.byu.cs.controller.CasController.*;
import static edu.byu.cs.controller.LogsController.logGet;
import static edu.byu.cs.controller.LogsController.logsGet;
import static edu.byu.cs.controller.SubmissionController.*;
import static spark.Spark.*;

public class Server {

    private static final Logger LOGGER = LoggerFactory.getLogger(Server.class);

    public static int setupEndpoints(int port) {
        port(port);

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

                post("/submit", adminRepoSubmitPost);

                path("/submissions", () -> {
                    get("/latest", latestSubmissionsGet);

                    get("/latest/:count", latestSubmissionsGet);

                    get("/active", submissionsActiveGet);

                    get("/student/:netID", studentSubmissionsGet);

                    post("/rerun", submissionsReRunPost);
                });

                path("/logs", () -> {
                    get("", logsGet);

                    get("/:log", logGet);
                });

                get("/test_mode", testModeGet);

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

        return port();
    }

    private static void setupProperties(String[] args) {
        Options options = getOptions();

        Properties properties = new Properties();

        CommandLineParser parser = new DefaultParser();
        try {
            CommandLine cmd = parser.parse(options, args);
            if (cmd.hasOption("db-host")) {
                properties.setProperty("db-host", cmd.getOptionValue("db-host"));
            }
            if (cmd.hasOption("db-port")) {
                properties.setProperty("db-port", cmd.getOptionValue("db-port"));
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
            if (cmd.hasOption("use-canvas")) {
                properties.setProperty("use-canvas", cmd.getOptionValue("use-canvas"));
            }
        } catch (ParseException e) {
            throw new RuntimeException("Error parsing command line arguments", e);
        }

        ApplicationProperties.loadProperties(properties);
    }

    private static Options getOptions() {
        Options options = new Options();
        options.addOption(null, "db-host", true, "Database Host");
        options.addOption(null, "db-port", true, "Database Port");
        options.addOption(null, "db-name", true, "Database Name");
        options.addOption(null, "db-user", true, "Database User");
        options.addOption(null, "db-pass", true, "Database Password");
        options.addOption(null, "frontend-url", true, "Frontend URL");
        options.addOption(null, "cas-callback-url", true, "CAS Callback URL");
        options.addOption(null, "canvas-token", true, "Canvas Token");
        options.addOption(null, "use-canvas", true, "Using Canvas");
        return options;
    }


    public static void main(String[] args) {
        ResourceUtils.copyResourceFiles("phases", new File(""));
        setupProperties(args);

        useSqlDaos();

        int port = setupEndpoints(8080);

        LOGGER.info("Server started on port {}", port);

        try {
            SubmissionController.reRunSubmissionsInQueue();
        } catch (IOException | DataAccessException e) {
            LOGGER.error("Error rerunning submissions already in queue", e);
        }
    }

    private static void useSqlDaos() {
        DaoService.setConfigurationDao(new ConfigurationSqlDao());
        DaoService.setQueueDao(new QueueSqlDao());
        DaoService.setRubricConfigDao(new RubricConfigSqlDao());
        DaoService.setSubmissionDao(new SubmissionSqlDao());
        DaoService.setUserDao(new UserSqlDao());
    }
}