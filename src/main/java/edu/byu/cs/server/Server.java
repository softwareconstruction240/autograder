package edu.byu.cs.server;

import edu.byu.cs.autograder.Grader;
import edu.byu.cs.autograder.TestAnalyzer;
import edu.byu.cs.controller.RestController;
import edu.byu.cs.controller.WebSocketController;

import static spark.Spark.init;
import static spark.Spark.port;

public class Server{

    private static final String ALL_PASS_REPO = "https://github.com/pawlh/chess-passing.git";
    private static final String ALL_FAIL_REPO = "https://github.com/softwareconstruction240/chess.git";
    public static void main(String[] args) {

        port(8080);
        WebSocketController.registerRoute();
        RestController.registerRoutes();
        init();
    }
}