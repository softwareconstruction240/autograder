package edu.byu.cs.controller;

import edu.byu.cs.controller.security.CasController;

import static edu.byu.cs.controller.security.JwtUtils.validateToken;
import static spark.Spark.*;

public class RestController {



    public static void registerRoutes() {

        staticFiles.location("/public");

        CasController.registerRoutes();

        // authenticated routes
        path("/api", () -> {
            before("/*", (req, res) -> {
                String token = req.cookie("token");
                if (token == null || !validateToken(token)) {
                    res.redirect("/login", 302);
                    halt(401);
                }
            });
        });

    }
}
