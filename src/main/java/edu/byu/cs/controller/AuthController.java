package edu.byu.cs.controller;

import com.google.gson.Gson;
import edu.byu.cs.dataAccess.DaoService;
import edu.byu.cs.dataAccess.UserDao;
import edu.byu.cs.model.User;
import spark.Filter;
import spark.Route;

import static edu.byu.cs.controller.JwtUtils.validateToken;
import static edu.byu.cs.model.User.Role.STUDENT;
import static spark.Spark.halt;

public class AuthController {
    /**
     * A filter that verifies that the request has a valid JWT in the Authorization header.
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

        UserDao userDao = DaoService.getUserDao();
        User user = userDao.getUser(netId);

        if (user == null) {
            halt(401, "You must register first.");
            return;
        }

        req.session().attribute("user", user);
    };

    public static Route registerPost = (req, res) -> {
        String token = req.cookie("token");

        if (token == null) {
            halt(401);
            return null;
        }
        String netId = validateToken(token);

        if (netId == null) {
            halt(401);
            return null;
        }

        String firstName = req.queryParams("firstName");
        String lastName = req.queryParams("lastName");
        String repoUrl = req.queryParams("repoUrl");

        if (firstName == null) {
            halt(400, "missing param `firstName`");
            return null;
        }
        if (lastName == null) {
            halt(400, "missing param `lastName`");
            return null;
        }
        if (repoUrl == null) {
            halt(400, "missing param `repoUrl`");
            return null;
        }

        UserDao userDao = DaoService.getUserDao();
        User newUser = new User(netId, firstName, lastName, repoUrl, STUDENT);
        try {
            userDao.insertUser(newUser);
        } catch (Exception e) {
            halt(409, "User already exists");
            return null;
        }

        res.status(200);
        return "";
    };

    public static Route meGet = (req, res) -> {
        User user = req.session().attribute("user");

        res.status(200);
        res.type("application/json");
        return new Gson().toJson(user);
    };

}
