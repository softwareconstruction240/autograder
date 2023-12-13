package edu.byu.cs.controller;

import static spark.Spark.*;

public class RestController {
    public static void registerRoutes() {

        //serve static files from resources /public. this is the official resources folder in java
        staticFiles.location("/public");

        post("/api/login", (req, res) -> {
            // ... validate credentials
            return null;
        });


        // authenticated routes
        path("/api", () -> {
            before("/*", (req, res) -> {
//              ... check if authenticated
//              halt(401, "You are not welcome here");

            });
            post("/logout", (req, res) -> "Bye World");


        });

    }
}
