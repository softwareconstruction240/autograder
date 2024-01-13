package edu.byu.cs.controller;

import com.google.gson.Gson;
import edu.byu.cs.controller.netmodel.RegisterRequest;
import edu.byu.cs.dataAccess.DaoService;
import edu.byu.cs.dataAccess.UserDao;
import edu.byu.cs.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Filter;
import spark.Route;

import static edu.byu.cs.controller.JwtUtils.validateToken;
import static edu.byu.cs.model.User.Role.STUDENT;
import static spark.Spark.halt;

public class AuthController {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthController.class);

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
            halt(403, "You must register first.");
            return;
        }

        req.session().attribute("user", user);
    };

    public static Filter verifyAdminMiddleware = (req, res) -> {
        User user = req.session().attribute("user");

        if (user.role() != User.Role.ADMIN) {
            halt(403);
        }
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

        RegisterRequest registerRequest = new Gson().fromJson(req.body(), RegisterRequest.class);

        if (registerRequest.firstName() == null) {
            halt(400, "missing param `firstName`");
            return null;
        }
        if (registerRequest.lastName() == null) {
            halt(400, "missing param `lastName`");
            return null;
        }
        if (registerRequest.repoUrl() == null) {
            halt(400, "missing param `repoUrl`");
            return null;
        }

        UserDao userDao = DaoService.getUserDao();
        User newUser = new User(netId, 0, registerRequest.firstName(), registerRequest.lastName(), registerRequest.repoUrl(), STUDENT);
        try {
            userDao.insertUser(newUser);
        } catch (Exception e) {
            halt(409, "User already exists");
            return null;
        }

        LOGGER.info("Registered " + newUser);
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
