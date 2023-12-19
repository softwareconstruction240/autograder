package edu.byu.cs.controller;

import spark.Filter;
import spark.Route;

import static edu.byu.cs.controller.JwtUtils.validateToken;
import static spark.Spark.halt;

public class AuthController {
    /**
     * A Spark filter that verifies that the request has a valid JWT in the Authorization header.
     * If the request is valid, the netId is added to the session for later use.
     */
    public static Filter verifyAuthenticatedMiddleware = (req, res) -> {
        String token = req.cookie("token");

        if (token == null) {
            halt(401);
            return;
        }
        String netId = validateToken(token);

        if (netId == null) {
            halt(401);
            return;
        }

        req.session().attribute("netId", netId);
    };

    public static Route meGet = (req, res) -> req.session().<String>attribute("netId");

}
