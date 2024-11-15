package edu.byu.cs.server;

import edu.byu.cs.autograder.GradingException;
import edu.byu.cs.controller.exception.*;
import edu.byu.cs.controller.WebSocketController;
import edu.byu.cs.dataAccess.DaoService;
import edu.byu.cs.dataAccess.DataAccessException;
import edu.byu.cs.properties.ApplicationProperties;
import edu.byu.cs.service.SubmissionService;
import edu.byu.cs.util.ResourceUtils;
import edu.byu.cs.util.Serializer;
import io.javalin.Javalin;
import io.javalin.http.ExceptionHandler;
import io.javalin.http.HandlerType;
import io.javalin.http.HttpStatus;
import org.apache.commons.cli.*;
import edu.byu.cs.server.endpointprovider.EndpointProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import static edu.byu.cs.controller.AdminController.*;
import static edu.byu.cs.controller.AuthController.*;
import static edu.byu.cs.controller.CasController.*;
import static edu.byu.cs.controller.ConfigController.*;
import static edu.byu.cs.controller.SubmissionController.*;
import static edu.byu.cs.controller.UserController.*;

import static io.javalin.apibuilder.ApiBuilder.*;

public class Server {

    private static Javalin app;

    private static final Logger LOGGER = LoggerFactory.getLogger(Server.class);
    private final EndpointProvider provider;

    public Server(EndpointProvider endpointProvider) {
        this.provider = endpointProvider;
    }

    public int start(int desiredPort) {
        int chosenPort = setupEndpoints(desiredPort);
        LOGGER.info("Server started on port {}", chosenPort);
        return chosenPort;
    }

    public int setupEndpoints(int port) {
        app = Javalin.create(config -> {
            config.staticFiles.add("/frontend/dist");

            config.jsonMapper(Serializer.jsonMapper);

            config.router.apiBuilder(() -> {
                path("/auth", () -> {
                    get("/callback", callbackGet);
                    get("/login", loginGet);

                    // TODO does Javalin guarantee this...?
                    // all routes after this point require authentication
                    post("/logout", logoutPost);
                });

                path("/api", () -> {
                    before("/*", ctx -> {
                        if (ctx.method() != HandlerType.OPTIONS)
                            verifyAuthenticatedMiddleware.handle(ctx);
                    });

                    patch("/repo", repoPatch);

                    get("/submit", submitGet);
                    post("/submit", submitPost);

                    get("/latest", latestSubmissionForMeGet);

                    get("/submission", submissionXGet);
                    get("/submission/{phase}", submissionXGet);

                    get("/me", meGet);

                    get("/config", getConfigStudent);

                    path("/admin", () -> {
                        before("/*", ctx -> {
                            if (ctx.method() != HandlerType.OPTIONS)
                                verifyAdminMiddleware.handle(ctx);
                        });

                        patch("/repo/{netId}", repoPatchAdmin);

                        get("/repo/history", repoHistoryAdminGet);

                        get("/users", usersGet);

                        post("/submit", adminRepoSubmitPost);

                        path("/submissions", () -> {
                            post("/approve", approveSubmissionPost);

                            get("/latest", latestSubmissionsGet);

                            get("/latest/{count}", latestSubmissionsGet);

                            get("/active", submissionsActiveGet);

                            get("/student/{netId}", studentSubmissionsGet);

                            post("/rerun", submissionsReRunPost);
                        });

                        get("/test_mode", testModeGet);

                        get("/analytics/commit", commitAnalyticsGet);

                        get("/analytics/commit/{option}", commitAnalyticsGet);

                        get("/honorChecker/zip/{section}", honorCheckerZipGet);

                        get("/sections", sectionsGet);

                        path("/config", () -> {
                            get("", getConfigAdmin);

                            post("/phases", updateLivePhases);
                            post("/banner", updateBannerMessage);

                            post("/courseIds", updateCourseIdsPost);
                            get("/courseIds", updateCourseIdsUsingCanvasGet);
                        });
                    });
                });

                get("/*", ctx -> {
                    if (ctx.path().equals("/ws")) // TODO Does this match?
                        return;

                    String urlParams = ctx.queryString();
                    urlParams = urlParams == null ? "" : "?" + urlParams;
                    ctx.redirect("/" + urlParams, HttpStatus.FOUND);
                });
            });
        })

                .ws("/ws", (wsConfig) -> {
                    wsConfig.onError(WebSocketController::onError);
                    wsConfig.onMessage(WebSocketController::onMessage);
                    // TODO Spark.webSocketIdleTimeoutMillis(300000);
                })

                .before(ctx -> {
                    ctx.header("Access-Control-Allow-Headers", "Authorization,Content-Type");
                    ctx.header("Access-Control-Allow-Methods", "GET,PUT,POST,DELETE,PATCH,OPTIONS");
                    ctx.header("Access-Control-Allow-Credentials", "true");
                    ctx.header("Access-Control-Allow-Origin", ApplicationProperties.frontendUrl());
                })

                .exception(BadRequestException.class, haltWithCode(400))
                .exception(UnauthorizedException.class, haltWithCode(401))
                .exception(ResourceForbiddenException.class, haltWithCode(403))
                .exception(ResourceNotFoundException.class, haltWithCode(404))
                .exception(WordOfWisdomViolationException.class, haltWithCode(418))
                .exception(UnprocessableEntityException.class, haltWithCode(422))
                .exception(Exception.class, haltWithCode(500))

                .start(port);

        return app.port();
    }

    private static <E extends Exception> ExceptionHandler<E> haltWithCode(int statusCode) {
        return (e, ctx) -> {
            ctx.status(statusCode);
            ctx.result(e.getMessage());
        };
    }
}
